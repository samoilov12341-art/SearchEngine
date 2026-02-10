package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteInfo;
import searchengine.repository.LemmasRepository;
import searchengine.repository.PagesRepository;
import searchengine.repository.SitesRepository;
import searchengine.services.SitesService;
import searchengine.services.StatisticsService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final PagesRepository pagesRepository;
    private final SitesRepository sitesRepository;
    private final LemmasRepository lemmasRepository;
    private final SitesService sitesService;
    @Setter
    public static AtomicBoolean indexingStatus;

    @Override
    public StatisticsResponse getStatistics() {
        sitesRepository.findAll().forEach(siteInfo -> {
            if (!sites.getSites().stream().map(Site::getUrl).toList().contains(siteInfo.getUrl())) {
                Site siteAdd = new Site();
                siteAdd.setName(siteInfo.getName());
                siteAdd.setUrl(siteInfo.getUrl());
                sites.getSites().add(siteAdd);
            }
        });

        indexingStatus = sitesService.getIndexingStatus();
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(indexingStatus.get());
        total.setPages(pagesRepository.findAll().size());
        total.setLemmas(lemmasRepository.findAll().size());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        sites.getSites().forEach(site -> {
            SiteInfo siteInfo = sitesRepository.findByUrl(site.getUrl());
            if (siteInfo != null) {
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(siteInfo.getName());
                item.setUrl(siteInfo.getUrl());
                item.setPages(pagesRepository.countPages(siteInfo.getId()));
                item.setLemmas(lemmasRepository.countLemmas(siteInfo.getId()));
                item.setStatus(siteInfo.getStatus().name());
                item.setError(siteInfo.getLastError());
                item.setStatusTime(siteInfo.getStatusTime());
                detailed.add(item);
            }
        });

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
