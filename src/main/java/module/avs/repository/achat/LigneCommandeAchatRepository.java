package module.avs.repository.achat;

import module.avs.model.achat.LigneCommandeAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneCommandeAchatRepository extends JpaRepository<LigneCommandeAchat, UUID> {
    List<LigneCommandeAchat> findByCommandeId(UUID commandeId);
}
