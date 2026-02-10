package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.aop.CheckSearch;
import searchengine.dto.response.IndexingResponse;
import searchengine.dto.response.Snippet;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteInfo;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmasRepository;
import searchengine.repository.PagesRepository;
import searchengine.repository.SitesRepository;
import searchengine.services.LemmaService;
import searchengine.services.SearchService;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaService lemmaService;
    private final LemmasRepository lemmasRepository;
    private final IndexesRepository indexesRepository;
    private final PagesRepository pagesRepository;
    private final SitesRepository sitesRepository;

    @Override
    @CheckSearch
    public IndexingResponse search(String query, String site, Integer offset, Integer limit) {
        List<Snippet> allSnippetsFromSites = new ArrayList<>();
        for (SiteInfo siteInfo : sitesRepository.findAll()) {
            List<Lemma> searchLem = lemmaService.searchLemmas(query).keySet().stream().map(lem ->
                            lemmasRepository.findLemmasByLemmaAndSiteId(lem, site != null
                                    ? sitesRepository.findByUrl(site).getId() : siteInfo.getId()))
                    .flatMap(Collection::stream).toList();
            if (searchLem.isEmpty()) {
                continue;
            }
            List<Lemma> sortedLemmasToSearch = searchLem.stream().
                    map(lemma -> new AbstractMap.SimpleEntry<>(lemma.getFrequency(), lemma)).
                    sorted(Comparator.comparingInt(Map.Entry::getKey)).
                    map(Map.Entry::getValue).collect(Collectors.toList());
            sortedLemmasToSearch.forEach(lemma -> {
                if (sortedLemmasToSearch.get(0).getFrequency() > 50) {
                    sortedLemmasToSearch.remove(0);
                }
            });

            Map<Integer, Index> searchPagesIndexes = indexesRepository.findAllByLemmaId(
                            sortedLemmasToSearch.get(0).getId())
                    .stream()
                    .collect(Collectors.toMap(Index::getPageId, index -> index));
            for (Lemma lemma : sortedLemmasToSearch) {
                if (!lemma.equals(sortedLemmasToSearch.get(0))) {
                    List<Integer> foundPagesAndSiteIds = new ArrayList<>();
                    List<Index> indexesNextLem = indexesRepository.findAllByLemmaId(lemma.getId());
                    indexesNextLem.forEach(index -> {
                        if (searchPagesIndexes.containsKey(index.getPageId())) {
                            foundPagesAndSiteIds.add(index.getPageId());
                        }
                    });
                    searchPagesIndexes.entrySet().removeIf(entry -> !foundPagesAndSiteIds.contains(entry.getKey()));
                }
            }
            if (searchPagesIndexes.isEmpty()) {
                continue;
            }
            List<Snippet> snippets = createSnippets(searchPagesIndexes.values(), sortedLemmasToSearch);
            if (site != null) {
                return new IndexingResponse(true, snippets.size(), snippets.stream().toList()
                        .subList(offset < snippets.size() ? offset : snippets.size(),
                                snippets.size()).stream().limit(limit).toList());
            }
            allSnippetsFromSites.addAll(snippets);
        }
        return new IndexingResponse(true, allSnippetsFromSites.size(), allSnippetsFromSites.stream().toList()
                .subList(offset < allSnippetsFromSites.size() ? offset : allSnippetsFromSites.size(),
                        allSnippetsFromSites.size()).stream().limit(limit).toList());
    }

    public List<Snippet> createSnippets(Collection<Index> indexes, List<Lemma> lemmasInput) {
        List<Snippet> snippets = new ArrayList<>();
        List<Float> allCountMatchesLem = new ArrayList<>();
        for (Index index : indexes) {
            try {
                Page page = pagesRepository.findById(index.getPageId()).orElseThrow();
                float allMatchesFromPage = 0;
                SiteInfo siteInfo = sitesRepository.findById(page.getSiteId()).orElseThrow();
                Document document = Jsoup.connect(page.getPath()).get();
                List<String> sentences = document.select("p").stream()
                        .map(element -> element.text().replaceAll("[\\p{So}\\p{Cn}]", "")).toList();
                Map<String, String> lemmasAndWords = lemmasAndWordsFromText(sentences);
                Map<String, Integer> snippetsBuild = searchSnippets(lemmasInput, lemmasAndWords, sentences);
                for (Map.Entry<String, Integer> entry : snippetsBuild.entrySet()) {
                    allMatchesFromPage += entry.getValue();
                }
                allCountMatchesLem.add(allMatchesFromPage);
                for (Map.Entry<String, Integer> entry : snippetsBuild.entrySet()) {
                    Snippet snippet = new Snippet();
                    snippet.setUri(page.getPath());
                    snippet.setSite(siteInfo.getUrl());
                    snippet.setSiteName(siteInfo.getName());
                    snippet.setTitle(document.title());
                    snippet.setSnippet(entry.getKey());
                    snippet.setRelevance(allMatchesFromPage);
                    snippets.add(snippet);
                }
            } catch (IOException e) {
                e.getLocalizedMessage();
            }
        }
        List<Float> sortedAllCountMatchesLem = allCountMatchesLem.stream()
                .sorted(Comparator.reverseOrder()).toList();
        snippets.forEach(snippet -> {
            Float countMatches = snippet.getRelevance() / sortedAllCountMatchesLem.get(0);
            DecimalFormat decimalFormat = new DecimalFormat( "#.#####" );
            Float result = Float.parseFloat(decimalFormat.format(countMatches).replaceAll(",","."));
            snippet.setRelevance(result);
        });
        return snippets;
    }

    public Map<String, String> lemmasAndWordsFromText(List<String> sentences) {
        Map<String, String> lemmasAndWord = new HashMap<>();
        for (String sentence : sentences) {
            String[] splitWords = sentence.replaceAll("[^а-яА-ЯёЁ]", " ")
                    .replaceAll("\\s+", " ").toLowerCase().split(" ");
            for (String word : splitWords) {
                if (!word.isBlank() && !lemmaService.checkParticleWord(word)) {
                    String lemma = lemmaService.getNormalsWord(word).toString();
                    lemmasAndWord.put(word, lemma);
                }
            }
        }
        return lemmasAndWord;
    }

    public Map<String, Integer> searchSnippets(List<Lemma> lemmasInput, Map<String, String> lemmasAndWords,
                                       List<String> sentences) {
        Map<String, Integer> snippetsAndCountMatches = new HashMap<>();
        List<String> searchWords = new ArrayList<>();
        for (Lemma lemmaInput : lemmasInput) {
            String[] splitLemmaInput = lemmaInput.getLemma().split(",");
            lemmasAndWords.forEach((word, lemma) -> {
                if (lemma.replaceAll("[^а-яё]", "").equals(splitLemmaInput[0]
                        .replaceAll("[^а-яё]", ""))
                        || splitLemmaInput.length > 1 && lemma.replaceAll("[^а-яё]","")
                        .contains(splitLemmaInput[1].replaceAll("[^а-яё]", ""))) {
                    searchWords.add(word);
                }
            });
        }
        for (String sentence : sentences) {
            if (sentence.startsWith(" ")) {
                sentence = sentence.replaceFirst("\\s+", "");
            }
            if (sentence.length() < 35 || sentence.contains("http://") || sentence.contains("https://")) {
                continue;
            }
            List<String> splitSentence = Stream.of(sentence.replaceAll("[^а-яА-ЯёЁa-zA-Z0-9]", " ")
                    .replaceAll("\\s+", " ")
                    .split(" ")).sorted(Comparator.comparingInt(String::length)).toList();
            List<String> checkReadyWords = new ArrayList<>();
            int countMatches = 0;
            for (String word : splitSentence) {
                for (String searchWord : searchWords) {
                    if (word.toLowerCase().equals(searchWord) && word.length() == searchWord.length()
                            && !checkReadyWords.contains(word)) {
                        checkReadyWords.add(word);
                        countMatches++;
                        String wordBold = "<b>" + word + "</b>";
                        sentence = sentence.replaceAll(word, wordBold);
                    }
                }
            }
            for (String schWord : searchWords) {
                if (sentence.contains(schWord) && !snippetsAndCountMatches.containsKey(sentence)) {
                    if (sentence.length() > 300 && sentence.substring(300).contains(schWord)) {
                        sentence = sentence.substring(Math.min(sentence.indexOf(schWord) - 150, sentence.indexOf(schWord)),
                                Math.min(sentence.indexOf(schWord) + 150, sentence.length()));
                        sentence = sentence.substring(sentence.indexOf(" ") + 1, sentence.lastIndexOf(" ") - 1);
                    } else if (sentence.length() > 300 && sentence.substring(0, 300).contains(schWord)){
                        sentence = sentence.substring(0, sentence.substring(0, 300).lastIndexOf(" "));

                    }
                    snippetsAndCountMatches.put(sentence, countMatches);
                }
            }
        }
        return snippetsAndCountMatches;
    }
}
