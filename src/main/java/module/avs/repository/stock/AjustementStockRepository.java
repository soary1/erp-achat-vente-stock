package module.avs.repository.stock;

import module.avs.model.stock.AjustementStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AjustementStockRepository extends JpaRepository<AjustementStock, UUID> {
    Optional<AjustementStock> findByNumero(String numero);
    List<AjustementStock> findByStatutCode(String statutCode);
    Page<AjustementStock> findAllByOrderByDateDemandeDesc(Pageable pageable);
    List<AjustementStock> findByDepotId(UUID depotId);
    List<AjustementStock> findByArticleId(UUID articleId);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(a.numero, 9) AS int)), 0) FROM AjustementStock a WHERE a.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    @Query("SELECT COUNT(a) FROM AjustementStock a WHERE a.statutCode = :statut")
    Long countByStatut(String statut);
    
    // Ajustements nécessitant double validation (montant élevé)
    @Query("SELECT a FROM AjustementStock a WHERE a.statutCode = 'APPROUVE_NIVEAU1' AND ABS(a.montantImpact) > :seuil ORDER BY a.dateDemande ASC")
    List<AjustementStock> findEnAttenteDoubleValidation(BigDecimal seuil);
}
