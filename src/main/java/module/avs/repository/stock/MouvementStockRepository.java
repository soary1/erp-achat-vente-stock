package module.avs.repository.stock;

import module.avs.model.stock.MouvementStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, UUID> {
    List<MouvementStock> findByArticleIdOrderByCreatedAtDesc(UUID articleId);
    List<MouvementStock> findByReferenceDoc(String referenceDoc);
    Page<MouvementStock> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT m FROM MouvementStock m WHERE m.createdAt BETWEEN :start AND :end ORDER BY m.createdAt DESC")
    List<MouvementStock> findByPeriod(OffsetDateTime start, OffsetDateTime end);
    
    @Query("SELECT m FROM MouvementStock m WHERE m.depotSource.id = :depotId OR m.depotDest.id = :depotId ORDER BY m.createdAt DESC")
    List<MouvementStock> findByDepot(UUID depotId);
}
