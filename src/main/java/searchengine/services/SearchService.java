package searchengine.services;

import searchengine.dto.response.IndexingResponse;

public interface SearchService {

    IndexingResponse search(String query, String site, Integer offset, Integer limit);
}
