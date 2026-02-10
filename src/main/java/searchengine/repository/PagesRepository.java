package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

@Repository
public interface PagesRepository extends JpaRepository<Page, Integer> {

    @Query("SELECT count(p) FROM Page p WHERE siteId = :siteId")
    Integer countPages(Integer siteId);
}
