package module.avs.repository.stock;

import module.avs.model.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {
    
    Optional<Stock> findByDepotIdAndArticleIdAndLotId(UUID depotId, UUID articleId, UUID lotId);
    
    Optional<Stock> findByDepotIdAndEmplacementIdAndArticleIdAndLotId(UUID depotId, UUID emplacementId, UUID articleId, UUID lotId);
    
    List<Stock> findByArticleId(UUID articleId);
    
    List<Stock> findByDepotId(UUID depotId);
    
    @Query("SELECT s FROM Stock s WHERE s.article.id = :articleId AND s.qtyReel > 0")
    List<Stock> findAvailableStockByArticle(UUID articleId);
    
    @Query("SELECT SUM(s.qtyReel) FROM Stock s WHERE s.article.id = :articleId")
    BigDecimal getTotalStockByArticle(UUID articleId);
    
    @Query("SELECT SUM(s.qtyReel) FROM Stock s WHERE s.depot.id = :depotId AND s.article.id = :articleId")
    BigDecimal getStockByDepotAndArticle(UUID depotId, UUID articleId);
    
    @Query("SELECT s FROM Stock s WHERE s.qtyReel - s.qtyReserve > 0 AND s.article.id = :articleId ORDER BY s.lot.datePeremption ASC NULLS LAST")
    List<Stock> findAvailableStockFEFO(UUID articleId);
    
    @Query("SELECT SUM(s.qtyReel * COALESCE(:unitCost, 0)) FROM Stock s WHERE s.depot.id = :depotId")
    BigDecimal getStockValueByDepot(UUID depotId, BigDecimal unitCost);
    
    @Modifying
    @Query("UPDATE Stock s SET s.qtyReel = s.qtyReel + :qty WHERE s.id = :stockId")
    void updateQtyReel(UUID stockId, BigDecimal qty);
    
    @Modifying
    @Query("UPDATE Stock s SET s.qtyReserve = s.qtyReserve + :qty WHERE s.id = :stockId")
    void updateQtyReserve(UUID stockId, BigDecimal qty);
}
