package module.avs.repository.organisation;

import module.avs.model.organisation.GroupeSociete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupeSocieteRepository extends JpaRepository<GroupeSociete, UUID> {
    Optional<GroupeSociete> findByCode(String code);
}
