package module.avs.repository.finance;

import module.avs.model.finance.FactureFournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FactureFournisseurRepository extends JpaRepository<FactureFournisseur, UUID> {
    Optional<FactureFournisseur> findByRefInterne(String refInterne);
    List<FactureFournisseur> findByFournisseurId(UUID fournisseurId);
    List<FactureFournisseur> findByStatutCode(String statutCode);
    Page<FactureFournisseur> findAllByOrderByDateFactureDesc(Pageable pageable);
    
    // Numérotation
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(f.refInterne, 9) AS int)), 0) FROM FactureFournisseur f WHERE f.refInterne LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    // Check if invoice already exists for a commande achat
    Optional<FactureFournisseur> findByCommandeAchatId(UUID commandeAchatId);
    
    // Find with details
    @Query("SELECT DISTINCT f FROM FactureFournisseur f LEFT JOIN FETCH f.fournisseur LEFT JOIN FETCH f.commandeAchat LEFT JOIN FETCH f.devise WHERE f.id = :id")
    Optional<FactureFournisseur> findByIdWithDetails(UUID id);
    
    @Query("SELECT f FROM FactureFournisseur f WHERE f.statutCode IN ('A_PAYER', 'PAYEE_PARTIEL') AND f.dateEcheance < :today")
    List<FactureFournisseur> findOverdueFactures(LocalDate today);
    
    @Query("SELECT SUM(f.montantTTC - f.montantPaye) FROM FactureFournisseur f WHERE f.statutCode IN ('A_PAYER', 'PAYEE_PARTIEL')")
    BigDecimal getTotalOutstandingAmount();
    
    @Query("SELECT SUM(f.montantTTC) FROM FactureFournisseur f WHERE f.dateFacture BETWEEN :start AND :end")
    BigDecimal sumByPeriod(LocalDate start, LocalDate end);
    
    // Stats for dashboard
    @Query("SELECT COUNT(f) FROM FactureFournisseur f WHERE f.statutCode = :statut")
    long countByStatut(String statut);
    
    // Factures récentes
    @Query("SELECT f FROM FactureFournisseur f WHERE f.dateFacture >= :start ORDER BY f.dateFacture DESC")
    List<FactureFournisseur> findRecentFactures(LocalDate start);
}
