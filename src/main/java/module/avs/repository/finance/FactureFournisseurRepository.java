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
    
    @Query("SELECT f FROM FactureFournisseur f WHERE f.statutCode IN ('A_PAYER', 'PAYEE_PARTIEL') AND f.dateEcheance < :today")
    List<FactureFournisseur> findOverdueFactures(LocalDate today);
    
    @Query("SELECT SUM(f.montantTTC - f.montantPaye) FROM FactureFournisseur f WHERE f.statutCode IN ('A_PAYER', 'PAYEE_PARTIEL')")
    BigDecimal getTotalOutstandingAmount();
}
