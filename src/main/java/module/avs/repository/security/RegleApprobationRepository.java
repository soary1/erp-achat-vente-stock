package module.avs.repository.security;

import module.avs.model.security.RegleApprobation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface RegleApprobationRepository extends JpaRepository<RegleApprobation, UUID> {
    
    @Query("SELECT r FROM RegleApprobation r WHERE r.documentType = :docType " +
           "AND (r.societe.id = :societeId OR r.societe IS NULL) " +
           "AND (r.minAmount <= :montant OR r.minAmount IS NULL) " +
           "AND (r.maxAmount >= :montant OR r.maxAmount IS NULL) " +
           "ORDER BY r.levelIndex")
    List<RegleApprobation> findApplicableRules(String docType, UUID societeId, BigDecimal montant);
    
    List<RegleApprobation> findByDocumentTypeOrderByLevelIndex(String documentType);
}
