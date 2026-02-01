package module.avs.repository.vente;

import module.avs.model.vente.BonLivraison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BonLivraisonRepository extends JpaRepository<BonLivraison, UUID> {
    Optional<BonLivraison> findByNumero(String numero);
    List<BonLivraison> findByCommandeId(UUID commandeId);
    Page<BonLivraison> findAllByOrderByDateExpeditionDesc(Pageable pageable);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(b.numero, 8) AS int)), 0) FROM BonLivraison b WHERE b.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    // Traçabilité : trouver les livraisons contenant un lot spécifique
    @Query("SELECT DISTINCT bl FROM BonLivraison bl JOIN bl.lignes l WHERE l.lot.id = :lotId ORDER BY bl.dateExpedition ASC")
    List<BonLivraison> findByLotId(UUID lotId);
}
