package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.exception.IndexingException;
import searchengine.model.Page;
import searchengine.model.SiteInfo;
import searchengine.repository.PagesRepository;
import searchengine.repository.SitesRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class IndexingPages extends RecursiveAction {

    private SiteInfo site;
    private PagesRepository pagesRepository;
    private SitesRepository sitesRepository;
    private PageService pageService;
    @Setter
    private static AtomicBoolean indexingStatus;
    private ConcurrentHashMap<String, Page> allReadyPagesUrl;
    private String path;

    @Override
    protected void compute() {
        if (allReadyPagesUrl.get(site.getUrl() + path) != null || !indexingStatus.get()) {
            return;
        }
        List<IndexingPages> allTasks = new ArrayList<>();
        SiteInfo siteInfo = sitesRepository.findById(site.getId()).orElseThrow();
        Page pageNew = new Page();
        pageNew.setPath(site.getUrl() + path);
        pageNew.setSiteId(site.getId());
        try {
            Document document = Jsoup.connect(site.getUrl() + path)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get();
            pageNew.setContent(document.html());
            pageNew.setCode(document.connection().response().statusCode());
            Elements elements = document.select("a[href]");
            elements.forEach(element -> {
                String href = element.attr("href");
                String[] hrefNotNul = href.split("");
                if (href.startsWith("/") && !href.contains(".webp") && hrefNotNul.length > 1) {
                    if (!indexingStatus.get() || allReadyPagesUrl.get(site.getUrl() + href) != null) {
                        return;
                    }
                    IndexingPages task = new IndexingPages(site, pagesRepository, sitesRepository, pageService,
                            allReadyPagesUrl, href);
                    task.fork();
                    allTasks.add(task);
                }
            });
        } catch (Exception e) {
            pagesRepository.save(pageNew);
            siteInfo.setStatusTime(LocalDateTime.now());
            siteInfo.setLastError(e.getLocalizedMessage());
            sitesRepository.save(siteInfo);
            return;
        }
        if (allReadyPagesUrl.get(site.getUrl() + path) != null || !indexingStatus.get()) {
            return;
        }
        allReadyPagesUrl.putIfAbsent(pageNew.getPath(), pageNew);
        if (!pageNew.getPath().replaceAll(siteInfo.getUrl(), "").isEmpty()) {
            pageService.indexesPageHtml(pagesRepository.save(pageNew));
            siteInfo.setStatusTime(LocalDateTime.now());
            sitesRepository.save(siteInfo);
        }
        try {
            allTasks.forEach(ForkJoinTask::join);
        } catch (Exception e) {
            throw new IndexingException(e.getLocalizedMessage());
        }
    }
}
