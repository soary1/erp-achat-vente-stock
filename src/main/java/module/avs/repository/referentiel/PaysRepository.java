package module.avs.repository.referentiel;

import module.avs.model.referentiel.Pays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaysRepository extends JpaRepository<Pays, String> {
}
