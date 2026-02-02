package module.avs.repository.finance;

import module.avs.model.finance.FactureClient;
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
public interface FactureClientRepository extends JpaRepository<FactureClient, UUID> {
    Optional<FactureClient> findByNumero(String numero);
    List<FactureClient> findByClientId(UUID clientId);
    List<FactureClient> findByStatutCode(String statutCode);
    Page<FactureClient> findAllByOrderByDateFactureDesc(Pageable pageable);
    
    // Numérotation
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(f.numero, 10) AS int)), 0) FROM FactureClient f WHERE f.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    // Check if invoice already exists for a bon de livraison
    Optional<FactureClient> findByBonLivraisonId(UUID bonLivraisonId);
    
    // Find all invoices with their related documents
    @Query("SELECT DISTINCT f FROM FactureClient f LEFT JOIN FETCH f.client LEFT JOIN FETCH f.commande LEFT JOIN FETCH f.bonLivraison WHERE f.id = :id")
    Optional<FactureClient> findByIdWithDetails(UUID id);
    
    @Query("SELECT f FROM FactureClient f WHERE f.statutCode IN ('A_PAYER', 'PAYEE_PARTIEL') AND f.dateEcheance < :today")
    List<FactureClient> findOverdueFactures(LocalDate today);
    
    @Query("SELECT SUM(f.montantTTC - f.montantEncaisse) FROM FactureClient f WHERE f.statutCode IN ('A_PAYER', 'PAYEE_PARTIEL')")
    BigDecimal getTotalOutstandingAmount();
    
    @Query("SELECT SUM(f.montantTTC) FROM FactureClient f WHERE f.dateFacture BETWEEN :start AND :end")
    BigDecimal sumByPeriod(LocalDate start, LocalDate end);
    
    // Stats for dashboard
    @Query("SELECT COUNT(f) FROM FactureClient f WHERE f.statutCode = :statut")
    long countByStatut(String statut);
    
    // Factures du mois
    @Query("SELECT f FROM FactureClient f WHERE f.dateFacture >= :start ORDER BY f.dateFacture DESC")
    List<FactureClient> findRecentFactures(LocalDate start);
}
