package module.avs.repository.vente;

import module.avs.model.vente.LigneBonLivraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneBonLivraisonRepository extends JpaRepository<LigneBonLivraison, UUID> {
    List<LigneBonLivraison> findByBonLivraisonId(UUID bonLivraisonId);
}
