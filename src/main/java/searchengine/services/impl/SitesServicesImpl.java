package searchengine.services.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.aop.CheckIndexing;
import searchengine.aop.CheckSearchAspect;
import searchengine.aop.CheckUrl;
import searchengine.config.SitesList;
import searchengine.dto.response.IndexingResponse;
import searchengine.model.*;
import searchengine.repository.PagesRepository;
import searchengine.repository.SitesRepository;
import searchengine.services.IndexingPages;
import searchengine.services.PageService;
import searchengine.services.SitesService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class SitesServicesImpl implements SitesService {

    @Getter
    private final AtomicBoolean indexingStatus = new AtomicBoolean(false);
    private final SitesList sites;
    private final SitesRepository sitesRepository;
    private final PagesRepository pagesRepository;
    private final PageService pageService;

    @Override
    @CheckIndexing
    public IndexingResponse startIndexing(boolean indexingStatus) {
        setIndexingStatus(indexingStatus);
        deleteSiteFromDb();
        createSite();
        indexingSites(null);
        return new IndexingResponse(true);
    }

    @Override
    @CheckIndexing
    public IndexingResponse stopIndexing(boolean indexingStatus) {
        setIndexingStatus(indexingStatus);
        return new IndexingResponse(true);
    }

    @Override
    public void setIndexingStatus(boolean indexingStatus) {
        this.indexingStatus.set(indexingStatus);
        IndexingPages.setIndexingStatus(this.indexingStatus);
        PageServiceImpl.setIndexingStatus(this.indexingStatus);
        CheckSearchAspect.setIndexingStatus(this.indexingStatus);
        StatisticsServiceImpl.setIndexingStatus(this.indexingStatus);
    }

    @Override
    @CheckUrl
    public IndexingResponse addOrUpdateSite(String url) {
        SiteInfo siteInfo = sitesRepository.findByUrl(url);
        if (siteInfo != null) {
            refreshSite(url);
        } else {
            sites.getSites().forEach(site -> {
                if (site.getUrl().equals(url)) {
                    SiteInfo siteNew = new SiteInfo();
                    siteNew.setName(site.getName());
                    siteNew.setUrl(site.getUrl());
                    siteNew.setStatus(IndexingStatus.INDEXING);
                    siteNew.setStatusTime(LocalDateTime.now());
                    setIndexingStatus(true);
                    indexingSites(sitesRepository.save(siteNew).getUrl());
                }
            });
        }
        return new IndexingResponse(true);
    }

    public void deleteSiteFromDb() {
        sites.getSites().forEach(siteConf -> {
            SiteInfo siteInfo = sitesRepository.findByUrl(siteConf.getUrl());
            if (siteInfo != null) {
                sitesRepository.deleteById(siteInfo.getId());
            }
        });
    }

    public void createSite() {
        sites.getSites().forEach(siteConf -> {
            SiteInfo siteInfo = sitesRepository.findByUrl(siteConf.getUrl());
            if (siteInfo == null) {
                SiteInfo site = new SiteInfo();
                site.setStatus(IndexingStatus.INDEXING);
                site.setName(siteConf.getName());
                site.setUrl(siteConf.getUrl());
                site.setStatusTime(LocalDateTime.now());
                sitesRepository.save(site);
            } else {
                siteInfo.setStatus(IndexingStatus.INDEXING);
                siteInfo.setStatusTime(LocalDateTime.now());
                sitesRepository.save(siteInfo);
            }
        });
    }

    @Override
    public void indexingSites(String url) {
        List<SiteInfo> sitesDb = new ArrayList<>();
        if (url == null) {
            sitesDb = sitesRepository.findAll();
        } else {
            sitesDb.add(sitesRepository.findByUrl(url));
        }
        ConcurrentHashMap<String, Page> pagesMap = new ConcurrentHashMap<>();
        List<Thread> allTasks = new ArrayList<>();
        sitesDb.forEach(site -> {
            Thread task = new Thread(() -> {
                SiteInfo siteInfo = sitesRepository.findById(site.getId()).orElseThrow();
                try {
                    new ForkJoinPool().invoke(new IndexingPages(
                            site, pagesRepository, sitesRepository, pageService, pagesMap, ""));
                } catch (Exception e) {
                    siteInfo.setStatus(IndexingStatus.FAILED);
                    siteInfo.setStatusTime(LocalDateTime.now());
                    siteInfo.setLastError(e.getLocalizedMessage());
                    sitesRepository.save(siteInfo);
                }
                if (!indexingStatus.get()) {
                    siteInfo.setStatus(IndexingStatus.FAILED);
                    siteInfo.setStatusTime(LocalDateTime.now());
                    siteInfo.setLastError("Индексация остановлена пользователем");
                    sitesRepository.save(siteInfo);
                } else {
                    siteInfo.setStatus(IndexingStatus.INDEXED);
                    sitesRepository.save(siteInfo);
                }
            });
            allTasks.add(task);
            task.start();
        });
        for (Thread thread : allTasks) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.getLocalizedMessage();
            }
        }
        this.indexingStatus.set(false);
    }

    public void refreshSite(String url) {
        sitesRepository.deleteById(sitesRepository.findByUrl(url).getId());
        sites.getSites().forEach(site -> {
            if (site.getUrl().equals(url)) {
                SiteInfo siteRefresh = new SiteInfo();
                siteRefresh.setName(site.getName());
                siteRefresh.setUrl(site.getUrl());
                siteRefresh.setStatus(IndexingStatus.INDEXING);
                siteRefresh.setStatusTime(LocalDateTime.now());
                setIndexingStatus(true);
                indexingSites(sitesRepository.save(siteRefresh).getUrl());
            }
        });
    }
}
