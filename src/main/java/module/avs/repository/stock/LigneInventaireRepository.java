package module.avs.repository.stock;

import module.avs.model.stock.LigneInventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneInventaireRepository extends JpaRepository<LigneInventaire, UUID> {
    List<LigneInventaire> findByInventaireId(UUID inventaireId);
    
    @Query("SELECT li FROM LigneInventaire li WHERE li.inventaire.id = :inventaireId AND li.estTraitee = false")
    List<LigneInventaire> findNonTraitees(UUID inventaireId);
    
    @Query("SELECT SUM(ABS(li.qtyReelleRetenue - li.qtyTheorique)) FROM LigneInventaire li WHERE li.inventaire.id = :inventaireId")
    BigDecimal getTotalEcart(UUID inventaireId);
}
