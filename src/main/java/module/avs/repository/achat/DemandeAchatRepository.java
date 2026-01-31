package module.avs.repository.achat;

import module.avs.model.achat.DemandeAchat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemandeAchatRepository extends JpaRepository<DemandeAchat, UUID> {
    Optional<DemandeAchat> findByNumero(String numero);
    List<DemandeAchat> findByDemandeurId(UUID demandeurId);
    List<DemandeAchat> findByStatutCode(String statutCode);
    @Query("SELECT d FROM DemandeAchat d LEFT JOIN FETCH d.site LEFT JOIN FETCH d.demandeur WHERE d.statutCode = :statutCode")
    List<DemandeAchat> findByStatutCodeWithDetails(String statutCode);
    Page<DemandeAchat> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(d.numero, 8) AS int)), 0) FROM DemandeAchat d WHERE d.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    @Query("SELECT d FROM DemandeAchat d LEFT JOIN FETCH d.site LEFT JOIN FETCH d.demandeur LEFT JOIN FETCH d.lignes l LEFT JOIN FETCH l.article WHERE d.id = :id")
    Optional<DemandeAchat> findByIdWithDetails(UUID id);
}
