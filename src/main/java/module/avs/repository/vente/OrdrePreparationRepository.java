package module.avs.repository.vente;

import module.avs.model.vente.OrdrePreparation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdrePreparationRepository extends JpaRepository<OrdrePreparation, UUID> {
    
    Optional<OrdrePreparation> findByNumero(String numero);
    List<OrdrePreparation> findByStatutCode(String statutCode);
    
    List<OrdrePreparation> findByStatutCodeOrderByPrioriteDescDateCreationAsc(String statutCode);
    
    List<OrdrePreparation> findByPreparateurId(UUID preparateurId);
    
    List<OrdrePreparation> findByCommandeId(UUID commandeId);
    
    @Query("SELECT MAX(CAST(SUBSTRING(o.numero, 8) AS int)) FROM OrdrePreparation o WHERE o.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
}
