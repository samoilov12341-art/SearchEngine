package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexesRepository extends JpaRepository<Index, Integer> {

    @Query(value = "SELECT * FROM indexes i WHERE i.page_id = :page_id and i.lemma_id = :lemma_id for update", nativeQuery = true)
    Index findByPageIdAndLemId(@Param("page_id") Integer pageId, @Param("lemma_id") Integer lemmaId);

    List<Index> findAllByLemmaId(Integer lemmaId);
}
