package module.avs.repository.referentiel;

import module.avs.model.referentiel.Devise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviseRepository extends JpaRepository<Devise, String> {
}
