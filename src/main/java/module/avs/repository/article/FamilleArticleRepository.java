package module.avs.repository.article;

import module.avs.model.article.FamilleArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilleArticleRepository extends JpaRepository<FamilleArticle, UUID> {
    Optional<FamilleArticle> findByCode(String code);
}
