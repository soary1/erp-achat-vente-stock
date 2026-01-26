package module.avs.repository.security;

import module.avs.model.security.DelegationAcces;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DelegationAccesRepository extends JpaRepository<DelegationAcces, UUID> {
    
    @Query("SELECT d FROM DelegationAcces d WHERE d.receveur.id = :receveurId AND d.startDate <= :now AND d.endDate >= :now")
    List<DelegationAcces> findActiveDelegationsForUser(UUID receveurId, OffsetDateTime now);
    
    List<DelegationAcces> findByDonneurId(UUID donneurId);
}
