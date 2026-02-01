package module.avs.repository.referentiel;

import module.avs.model.referentiel.MotifRetour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotifRetourRepository extends JpaRepository<MotifRetour, String> {
}
