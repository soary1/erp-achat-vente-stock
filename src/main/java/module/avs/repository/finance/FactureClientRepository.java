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
    
    @Query("SELECT f FROM FactureClient f WHERE f.statutCode IN ('A_PAYER', 'PAYEE_PARTIEL') AND f.dateEcheance < :today")
    List<FactureClient> findOverdueFactures(LocalDate today);
    
    @Query("SELECT SUM(f.montantTTC - f.montantEncaisse) FROM FactureClient f WHERE f.statutCode IN ('A_PAYER', 'PAYEE_PARTIEL')")
    BigDecimal getTotalOutstandingAmount();
    
    @Query("SELECT SUM(f.montantTTC) FROM FactureClient f WHERE f.dateFacture BETWEEN :start AND :end")
    BigDecimal sumByPeriod(LocalDate start, LocalDate end);
}
