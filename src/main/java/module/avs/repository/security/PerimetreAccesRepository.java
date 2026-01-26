package module.avs.repository.security;

import module.avs.model.security.PerimetreAcces;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PerimetreAccesRepository extends JpaRepository<PerimetreAcces, UUID> {
    List<PerimetreAcces> findByUtilisateurIdAndActiveTrue(UUID utilisateurId);
}
