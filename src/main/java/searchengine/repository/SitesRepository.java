package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteInfo;

@Repository
public interface SitesRepository extends JpaRepository<SiteInfo, Integer> {

    SiteInfo findByUrl(String url);

    void deleteByUrl(String url);
}
