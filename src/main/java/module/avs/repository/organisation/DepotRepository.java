package module.avs.repository.organisation;

import module.avs.model.organisation.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepotRepository extends JpaRepository<Depot, UUID> {
    Optional<Depot> findByCode(String code);
    List<Depot> findBySiteIdAndIsActiveTrue(UUID siteId);
    List<Depot> findBySiteId(UUID siteId);
    List<Depot> findByIsActiveTrue();
    
    @Query("SELECT d FROM Depot d LEFT JOIN FETCH d.site WHERE d.id = :id")
    Optional<Depot> findByIdWithSite(@Param("id") UUID id);
}
