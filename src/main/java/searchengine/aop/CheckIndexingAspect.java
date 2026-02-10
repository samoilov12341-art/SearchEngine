package searchengine.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exception.IndexingException;
import searchengine.exception.InputException;
import searchengine.exception.NotFoundException;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteInfo;
import searchengine.repository.SitesRepository;
import searchengine.services.SitesService;

import java.io.IOException;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class CheckIndexingAspect {

    public final SitesService sitesService;
    private final SitesRepository sitesRepository;
    private final SitesList sites;

    @Before(value = "@annotation(CheckIndexing)")
    public void checkIndexing(JoinPoint joinPoint) {
        boolean indStatus = getIndSt(joinPoint.getArgs()[0]);
        if (sitesService.getIndexingStatus().get() && indStatus) {
            throw new IndexingException("Индексация уже запущена");
        }
        if (!sitesService.getIndexingStatus().get() && !indStatus) {
            throw new IndexingException("Индексация не запущена");
        }
    }

    @Before(value = "@annotation(CheckUrl)")
    public void checkUrl(JoinPoint joinPoint) {
        String regex1 = "https://www\\.";
        String regex2 = "http://www\\.";
        String url = getUrl(joinPoint.getArgs()[0]);
        if (url != null && !url.isBlank()) {
            if (sitesService.getIndexingStatus().get()) {
                throw new IndexingException("Пожалуйста подождите, идёт индексация страниц");
            }
            if (!url.startsWith(regex1) && !url.replaceAll(regex1, "").contains(".")
                    || !url.startsWith(regex2) && !url.replaceAll(regex2, "").contains(".")) {
                throw new IndexingException("Неверный формат ввода url");
            }
            if (sites.getSites().stream().noneMatch(site -> site.getUrl().equals(url))) {
                addSite(url);
            }
        }
    }

    private String getUrl(Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        }
        return null;
    }

    private Boolean getIndSt(Object arg) {
        if (arg instanceof Boolean) {
            return (Boolean) arg;
        }
        return null;
    }

    public void addSite(String url) {
        try {
            Connection.Response response = Jsoup.connect(url).execute();
            if (response.statusCode() != 200) {
                throw new NotFoundException("Сайт не найден");
            } else if (response.statusCode() == 200){
                Site site = new Site();
                site.setUrl(url);
                site.setName(url.replaceAll("https://www\\.", ""));
                sites.getSites().add(site);
            }
        } catch (IOException e) {
            e.getLocalizedMessage();
        }
    }
}
