package module.avs.repository.vente;

import module.avs.model.vente.CommandeClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommandeClientRepository extends JpaRepository<CommandeClient, UUID> {
    Optional<CommandeClient> findByNumero(String numero);
    List<CommandeClient> findByClientId(UUID clientId);
    List<CommandeClient> findByStatutCode(String statutCode);
    Page<CommandeClient> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(c.numero, 9) AS int)), 0) FROM CommandeClient c WHERE c.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    @Query("SELECT SUM(c.totalTTC) FROM CommandeClient c WHERE c.createdAt BETWEEN :start AND :end")
    BigDecimal sumTotalByPeriod(OffsetDateTime start, OffsetDateTime end);
    
    @Query("SELECT COUNT(c) FROM CommandeClient c WHERE c.statutCode = :statut")
    Long countByStatut(String statut);
}
