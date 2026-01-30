package module.avs.repository.vente;

import module.avs.model.vente.ReservationStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationStockRepository extends JpaRepository<ReservationStock, UUID> {
    List<ReservationStock> findByLigneCommandeId(UUID ligneCommandeId);
    List<ReservationStock> findByArticleIdAndDepotId(UUID articleId, UUID depotId);
}
