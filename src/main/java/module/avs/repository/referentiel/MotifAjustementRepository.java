package module.avs.repository.referentiel;

import module.avs.model.referentiel.MotifAjustement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotifAjustementRepository extends JpaRepository<MotifAjustement, String> {
}
