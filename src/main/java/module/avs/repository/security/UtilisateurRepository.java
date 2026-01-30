package module.avs.repository.security;

import module.avs.model.security.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {
    Optional<Utilisateur> findByUsername(String username);
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findByIsActiveTrue();
    List<Utilisateur> findByDepartementId(UUID departementId);
    
    @Query("SELECT u FROM Utilisateur u JOIN u.roles r WHERE r.code = :roleCode")
    List<Utilisateur> findByRoleCode(String roleCode);
}
