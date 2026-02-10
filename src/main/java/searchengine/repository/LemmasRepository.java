package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmasRepository extends JpaRepository<Lemma, Integer> {

    @Query(value = "SELECT * FROM lemmas i WHERE i.lemma = :lemma and i.site_id = :site_id for update", nativeQuery = true)
    Lemma findAllByLemmaAndSiteId(@Param("lemma") String lemma, @Param("site_id") Integer siteId);

    @Query(value = "SELECT l FROM Lemma l WHERE l.lemma = :lemma and l.siteId = :siteId")
    List<Lemma> findLemmasByLemmaAndSiteId(String lemma, Integer siteId);

    @Query("SELECT count(l) FROM Lemma l WHERE siteId = :siteId")
    Integer countLemmas(Integer siteId);
}
