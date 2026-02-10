package searchengine.services;

import searchengine.dto.response.IndexingResponse;

import java.util.concurrent.atomic.AtomicBoolean;

public interface SitesService {

    IndexingResponse startIndexing(boolean indexingStatus);

    IndexingResponse stopIndexing(boolean indexingStatus);

    IndexingResponse addOrUpdateSite(String url);

    AtomicBoolean getIndexingStatus();

    void setIndexingStatus(boolean indexingStatus);

    void indexingSites(String url);
}
