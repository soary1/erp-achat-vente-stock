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
    
    List<Stock> findByDepotIdAndArticleId(UUID depotId, UUID articleId);
    
    @Query("SELECT s FROM Stock s WHERE s.article.id = :articleId AND s.qtyReel > 0")
    List<Stock> findAvailableStockByArticle(UUID articleId);
    
    @Query("SELECT SUM(s.qtyReel) FROM Stock s WHERE s.article.id = :articleId")
    BigDecimal getTotalStockByArticle(UUID articleId);
    
    @Query("SELECT SUM(s.qtyReel) FROM Stock s WHERE s.depot.id = :depotId")
    BigDecimal getTotalStockByDepot(UUID depotId);
    
    @Query("SELECT SUM(s.qtyReel) FROM Stock s WHERE s.depot.id = :depotId AND s.article.id = :articleId")
    BigDecimal getStockByDepotAndArticle(UUID depotId, UUID articleId);
    
    @Query("SELECT s FROM Stock s WHERE s.qtyReel - s.qtyReserve > 0 AND s.article.id = :articleId ORDER BY s.lot.datePeremption ASC NULLS LAST")
    List<Stock> findAvailableStockFEFO(UUID articleId);
    
    // Allocation FIFO : par date d'entrée la plus ancienne (via ID stock créé en premier)
    @Query("SELECT s FROM Stock s WHERE s.qtyReel - s.qtyReserve > 0 AND s.article.id = :articleId ORDER BY s.id ASC")
    List<Stock> findAvailableStockFIFO(UUID articleId);
    
    @Query("SELECT SUM(s.qtyReel * COALESCE(:unitCost, 0)) FROM Stock s WHERE s.depot.id = :depotId")
    BigDecimal getStockValueByDepot(UUID depotId, BigDecimal unitCost);
    
    @Modifying
    @Query("UPDATE Stock s SET s.qtyReel = s.qtyReel + :qty WHERE s.id = :stockId")
    void updateQtyReel(UUID stockId, BigDecimal qty);
    
    @Modifying
    @Query("UPDATE Stock s SET s.qtyReserve = s.qtyReserve + :qty WHERE s.id = :stockId")
    void updateQtyReserve(UUID stockId, BigDecimal qty);
    
    @Query("SELECT s FROM Stock s LEFT JOIN FETCH s.depot LEFT JOIN FETCH s.article WHERE s.qtyReel > 0")
    List<Stock> findAllStocksWithDepotAndArticle();
    
    // Méthodes pour calcul de prix selon méthode de valorisation
    List<Stock> findByDepotIdAndArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationAsc(UUID depotId, UUID articleId, BigDecimal qty);
    
    List<Stock> findByArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationAsc(UUID articleId, BigDecimal qty);
    
    List<Stock> findByDepotIdAndArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationDesc(UUID depotId, UUID articleId, BigDecimal qty);
    
    List<Stock> findByArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationDesc(UUID articleId, BigDecimal qty);
    
    List<Stock> findByDepotIdAndArticleIdAndQtyReelGreaterThan(UUID depotId, UUID articleId, BigDecimal qty);
    
    List<Stock> findByArticleIdAndQtyReelGreaterThan(UUID articleId, BigDecimal qty);
    
    // Requête avec filtres (depot, famille, valorisation, search)
    @Query(value = """
        SELECT s.* FROM stock s
        LEFT JOIN depot d ON d.id = s.depot_id
        LEFT JOIN article a ON a.id = s.article_id
        LEFT JOIN famille_article f ON f.id = a.famille_id
        LEFT JOIN methode_valorisation m ON m.code = f.methode_valorisation_code
        LEFT JOIN lot l ON l.id = s.lot_id
        LEFT JOIN emplacement e ON e.id = s.emplacement_id
        WHERE (CAST(:depotId AS uuid) IS NULL OR s.depot_id = CAST(:depotId AS uuid))
          AND (CAST(:familleId AS uuid) IS NULL OR a.famille_id = CAST(:familleId AS uuid))
          AND (CAST(:methodeCode AS varchar) IS NULL OR f.methode_valorisation_code = CAST(:methodeCode AS varchar))
          AND (CAST(:search AS varchar) IS NULL OR CAST(:search AS varchar) = '' 
               OR LOWER(a.sku) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%')) 
               OR LOWER(a.label) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%')))
        ORDER BY a.label, d.name
        """, nativeQuery = true)
    List<Stock> findStocksFiltered(UUID depotId, UUID familleId, String methodeCode, String search);
}
