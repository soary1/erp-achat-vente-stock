package module.avs.repository.achat;

import module.avs.model.achat.LigneDemandeAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneDemandeAchatRepository extends JpaRepository<LigneDemandeAchat, UUID> {
    List<LigneDemandeAchat> findByDemandeAchatId(UUID demandeAchatId);
}
