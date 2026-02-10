package searchengine.services;

import java.util.List;
import java.util.Map;

public interface LemmaService {

    Map<String, Integer> searchLemmas(String htmlText);

    List<String> getNormalsWord(String word);

    boolean checkParticleWord(String morphInfo);
}
