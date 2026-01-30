package module.avs.repository.finance;

import module.avs.model.finance.EncaissementClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EncaissementClientRepository extends JpaRepository<EncaissementClient, UUID> {
    List<EncaissementClient> findByFactureId(UUID factureId);
    
    @Query("SELECT SUM(e.montant) FROM EncaissementClient e WHERE e.dateEncaissement BETWEEN :start AND :end")
    BigDecimal sumEncaissementsByPeriod(LocalDate start, LocalDate end);
}
