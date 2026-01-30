package module.avs.repository.security;

import module.avs.model.security.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, UUID> {
    Optional<Departement> findByCode(String code);
}
