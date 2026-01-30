package module.avs.repository.referentiel;

import module.avs.model.referentiel.UniteMesure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UniteMesureRepository extends JpaRepository<UniteMesure, String> {
}
