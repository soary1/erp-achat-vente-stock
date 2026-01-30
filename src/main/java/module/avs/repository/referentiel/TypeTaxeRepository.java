package module.avs.repository.referentiel;

import module.avs.model.referentiel.TypeTaxe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeTaxeRepository extends JpaRepository<TypeTaxe, String> {
}
