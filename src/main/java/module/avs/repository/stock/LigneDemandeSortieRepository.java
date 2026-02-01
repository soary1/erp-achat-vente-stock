package module.avs.repository.stock;

import module.avs.model.stock.LigneDemandeSortie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneDemandeSortieRepository extends JpaRepository<LigneDemandeSortie, UUID> {
    List<LigneDemandeSortie> findByDemandeId(UUID demandeId);
    List<LigneDemandeSortie> findByArticleId(UUID articleId);
    List<LigneDemandeSortie> findByLotId(UUID lotId);
}
