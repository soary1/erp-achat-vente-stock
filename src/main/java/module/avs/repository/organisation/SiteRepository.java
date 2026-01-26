package module.avs.repository.organisation;

import module.avs.model.organisation.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteRepository extends JpaRepository<Site, UUID> {
    Optional<Site> findByCode(String code);
    List<Site> findBySocieteIdAndIsActiveTrue(UUID societeId);
    List<Site> findByIsActiveTrue();
}
