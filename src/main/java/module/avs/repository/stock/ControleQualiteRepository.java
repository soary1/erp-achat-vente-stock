package module.avs.repository.stock;

import module.avs.model.stock.ControleQualite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ControleQualiteRepository extends JpaRepository<ControleQualite, UUID> {
    List<ControleQualite> findByLigneReceptionId(UUID ligneReceptionId);
}
