package module.avs.repository.referentiel;

import module.avs.model.referentiel.MethodeValorisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MethodeValorisationRepository extends JpaRepository<MethodeValorisation, String> {
}
