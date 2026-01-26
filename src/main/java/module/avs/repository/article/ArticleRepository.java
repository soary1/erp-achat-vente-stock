package module.avs.repository.article;

import module.avs.model.article.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
    Optional<Article> findBySku(String sku);
    List<Article> findByIsActiveTrue();
    List<Article> findBySocieteIdAndIsActiveTrue(UUID societeId);
    List<Article> findByFamilleIdAndIsActiveTrue(UUID familleId);
    
    @Query("SELECT a FROM Article a WHERE a.isActive = true AND (LOWER(a.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.label) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Article> searchArticles(String search, Pageable pageable);
}
