package module.avs.repository.vente;

import module.avs.model.vente.RetourClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetourClientRepository extends JpaRepository<RetourClient, UUID> {
    Optional<RetourClient> findByNumero(String numero);
    List<RetourClient> findByClientId(UUID clientId);
    List<RetourClient> findByStatutCode(String statutCode);
    Page<RetourClient> findAllByOrderByDateDemandeDesc(Pageable pageable);
    List<RetourClient> findByCommandeId(UUID commandeId);
    List<RetourClient> findByFactureId(UUID factureId);
    List<RetourClient> findByBonLivraisonId(UUID bonLivraisonId);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(r.numero, 9) AS int)), 0) FROM RetourClient r WHERE r.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    @Query("SELECT COUNT(r) FROM RetourClient r WHERE r.statutCode = :statut")
    Long countByStatut(String statut);
}
