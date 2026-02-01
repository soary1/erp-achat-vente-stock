package module.avs.repository.organisation;

import module.avs.model.organisation.Emplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmplacementRepository extends JpaRepository<Emplacement, UUID> {
    Optional<Emplacement> findByCode(String code);
    List<Emplacement> findByDepotId(UUID depotId);
    
    @Query("SELECT e FROM Emplacement e JOIN FETCH e.depot ORDER BY e.code")
    List<Emplacement> findAllWithDepot();
}
