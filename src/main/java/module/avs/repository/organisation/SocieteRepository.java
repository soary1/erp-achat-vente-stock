package module.avs.repository.organisation;

import module.avs.model.organisation.Societe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocieteRepository extends JpaRepository<Societe, UUID> {
    Optional<Societe> findByCode(String code);
}
