package module.avs.repository.vente;

import module.avs.model.vente.LigneCommandeClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneCommandeClientRepository extends JpaRepository<LigneCommandeClient, UUID> {
    List<LigneCommandeClient> findByCommandeId(UUID commandeId);
}
