package searchengine.services.impl;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LemmaServiceImpl implements LemmaService {

    private final LuceneMorphology luceneMorphology;

    {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Integer> searchLemmas(String htmlText) {
        Map<String, Integer> lemmasMap = new HashMap<>();
        List<String> textList = searchWords(htmlText);

        for (String word : textList) {
            if (word.isBlank() || chekCorrectRussianWord(word)) {
                continue;
            }
            String morphInfo = luceneMorphology.getMorphInfo(word).toString();
            if (checkParticleWord(morphInfo)) {
                continue;
            }
            String normalsLemWord = getNormalsWord(word).toString();
            if (normalsLemWord.isEmpty()) {
                continue;
            }
            if (lemmasMap.containsKey(normalsLemWord)) {
                lemmasMap.put(normalsLemWord, lemmasMap.get(normalsLemWord) + 1);
            } else {
                lemmasMap.put(normalsLemWord, 1);
            }
        }
        return lemmasMap;
    }

    @Override
    public List<String> getNormalsWord(String word) {
        return luceneMorphology.getNormalForms(word);
    }

    public List<String> searchWords(String text) {
        String[] splitText = Jsoup.parse(text)
                .text()
                .replaceAll("[^а-яА-ЯёЁ]", " ").replaceAll("\\s+", " ")
                .toLowerCase()
                .split("\\s+");
        List<String> words = new ArrayList<>(List.of(splitText));
        words.removeIf(String::isEmpty);
        words.removeIf(s -> s.length() < 2);

        return words;
    }

    @Override
    public boolean checkParticleWord(String morphInfo) {
        if (morphInfo.toUpperCase().contains("МЕЖД")
                || morphInfo.toUpperCase().contains("ПРЕДЛ")
                || morphInfo.toUpperCase().contains("СОЮЗ")) {
            return true;
        }
        return false;
    }

    public boolean chekCorrectRussianWord(String word) {
        String wordInfo = luceneMorphology.getMorphInfo(word).toString();
        if (wordInfo.matches("\\W\\w&&[^а-яА-ЯёЁ\\s]")) {
            return true;
        }
        return false;
    }
}
