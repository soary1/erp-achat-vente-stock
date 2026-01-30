package module.avs.repository.article;

import module.avs.model.article.LigneListeTarifaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LigneListeTarifaireRepository extends JpaRepository<LigneListeTarifaire, UUID> {
    List<LigneListeTarifaire> findByListeTarifaireId(UUID listeTarifaireId);
    
    @Query("SELECT l FROM LigneListeTarifaire l WHERE l.listeTarifaire.id = :listeTarifaireId " +
           "AND l.article.id = :articleId AND l.startDate <= :date " +
           "AND (l.endDate IS NULL OR l.endDate >= :date) ORDER BY l.startDate DESC")
    Optional<LigneListeTarifaire> findActivePrice(UUID listeTarifaireId, UUID articleId, LocalDate date);
}
