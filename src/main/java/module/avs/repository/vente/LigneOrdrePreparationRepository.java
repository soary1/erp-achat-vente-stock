package module.avs.repository.vente;

import module.avs.model.vente.LigneOrdrePreparation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneOrdrePreparationRepository extends JpaRepository<LigneOrdrePreparation, UUID> {
    
    List<LigneOrdrePreparation> findByOrdreId(UUID ordreId);
    
    List<LigneOrdrePreparation> findByOrdreIdAndScanneTrue(UUID ordreId);
    
    List<LigneOrdrePreparation> findByOrdreIdAndScanneFalse(UUID ordreId);
}
