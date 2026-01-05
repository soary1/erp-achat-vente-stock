package com.erp.achats.ventes.repository;

import com.erp.achats.ventes.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    
    List<StockMovement> findByProductId(Long productId);
    
    List<StockMovement> findByMovementDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<StockMovement> findByType(StockMovement.MovementType type);
}
