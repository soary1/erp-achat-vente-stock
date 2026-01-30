package module.avs.repository.vente;

import module.avs.model.vente.LigneDevisClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneDevisClientRepository extends JpaRepository<LigneDevisClient, UUID> {
    List<LigneDevisClient> findByDevisId(UUID devisId);
}
