package module.avs.repository.achat;

import module.avs.model.achat.CommandeAchat;
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
public interface CommandeAchatRepository extends JpaRepository<CommandeAchat, UUID> {
    Optional<CommandeAchat> findByNumero(String numero);
    List<CommandeAchat> findByStatutCode(String statutCode);
    @Query("SELECT c FROM CommandeAchat c LEFT JOIN FETCH c.fournisseur LEFT JOIN FETCH c.site LEFT JOIN FETCH c.devise LEFT JOIN FETCH c.acheteur WHERE c.statutCode = :statutCode")
    List<CommandeAchat> findByStatutCodeWithDetails(String statutCode);
    List<CommandeAchat> findByFournisseurId(UUID fournisseurId);
    @Query("SELECT c FROM CommandeAchat c LEFT JOIN FETCH c.fournisseur LEFT JOIN FETCH c.site LEFT JOIN FETCH c.devise ORDER BY c.dateCommande DESC")
    Page<CommandeAchat> findAllWithDetails(Pageable pageable);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(c.numero, 8) AS int)), 0) FROM CommandeAchat c WHERE c.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    @Query("SELECT SUM(c.totalTTC) FROM CommandeAchat c WHERE c.dateCommande BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalByPeriod(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT c FROM CommandeAchat c LEFT JOIN FETCH c.fournisseur LEFT JOIN FETCH c.site LEFT JOIN FETCH c.devise LEFT JOIN FETCH c.acheteur LEFT JOIN FETCH c.lignes l LEFT JOIN FETCH l.article WHERE c.id = :id")
    Optional<CommandeAchat> findByIdWithDetails(UUID id);
    
    @Query("SELECT COUNT(c) FROM CommandeAchat c WHERE c.statutCode = :statut")
    Long countByStatut(String statut);
}
