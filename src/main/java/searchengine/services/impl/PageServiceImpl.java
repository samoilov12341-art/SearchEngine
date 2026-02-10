package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmasRepository;
import searchengine.repository.SitesRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageService;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final LemmaService lemmaService;
    private final LemmasRepository lemmasRepository;
    private final IndexesRepository indexesRepository;
    private final SitesRepository sitesRepository;
    @Setter
    private static AtomicBoolean indexingStatus;

    @Override
    public void indexesPageHtml(Page page) {
        try {
            Document document = Jsoup.parse(page.getContent());
            Map<String, Integer> lemmas = lemmaService.searchLemmas(document.select("p").text());
            lemmas.entrySet().parallelStream().forEach(entry -> createLemma(page, entry.getKey(), entry.getValue()));
        } catch (Exception e) {
            e.getLocalizedMessage();
        }
    }

    public void createLemma(Page page, String lemma, Integer count) {
        if (!indexingStatus.get()) {
            return;
        }
        Lemma lemmaDb = lemmasRepository.findAllByLemmaAndSiteId(lemma, page.getSiteId());
        if (lemmaDb != null) {
            lemmaDb.setFrequency(lemmaDb.getFrequency() + 1);
            createIndex(page, lemmasRepository.save(lemmaDb), count);
        } else {
            Lemma lemmaNew = new Lemma();
            lemmaNew.setSiteId(page.getSiteId());
            lemmaNew.setLemma(lemma);
            lemmaNew.setFrequency(1);
            createIndex(page, lemmasRepository.save(lemmaNew), count);
        }
    }

    public void createIndex(Page page, Lemma lemma, Integer rankLem) {
        try {
            Index indexDb = indexesRepository.findByPageIdAndLemId(page.getId(), lemma.getId());
            if (indexDb != null) {
                indexDb.setRankLem(indexDb.getRankLem() + rankLem);
                indexesRepository.save(indexDb);
            } else {
                Index indexNew = new Index();
                indexNew.setPageId(page.getId());
                indexNew.setLemmaId(lemma.getId());
                indexNew.setRankLem(Float.valueOf(rankLem));
                indexesRepository.save(indexNew);
            }
        } catch (Exception e) {
            e.getLocalizedMessage();
        }
    }
}
