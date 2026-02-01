package module.avs.repository.stock;

import module.avs.model.stock.LigneTransfertStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneTransfertStockRepository extends JpaRepository<LigneTransfertStock, UUID> {
    List<LigneTransfertStock> findByTransfertId(UUID transfertId);
    List<LigneTransfertStock> findByArticleId(UUID articleId);
    List<LigneTransfertStock> findByLotId(UUID lotId);
}
