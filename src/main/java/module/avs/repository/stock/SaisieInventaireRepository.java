package module.avs.repository.stock;

import module.avs.model.stock.SaisieInventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SaisieInventaireRepository extends JpaRepository<SaisieInventaire, UUID> {
    List<SaisieInventaire> findByLigneInventaireIdOrderByTourComptage(UUID ligneInventaireId);
}
