package com.erp.achats.ventes.service;

import com.erp.achats.ventes.model.Product;
import com.erp.achats.ventes.model.StockMovement;
import com.erp.achats.ventes.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;

    public List<StockMovement> getAllStockMovements() {
        return stockMovementRepository.findAll();
    }

    public List<StockMovement> getStockMovementsByProduct(Long productId) {
        return stockMovementRepository.findByProductId(productId);
    }

    public List<StockMovement> getStockMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stockMovementRepository.findByMovementDateBetween(startDate, endDate);
    }

    public List<StockMovement> getStockMovementsByType(StockMovement.MovementType type) {
        return stockMovementRepository.findByType(type);
    }

    @Transactional
    public StockMovement createStockMovement(Long productId, StockMovement.MovementType type, 
                                             Integer quantity, String referenceNumber, String reason) {
        Product product = productService.getProductById(productId);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantity(quantity);
        
        // Calculate stock before/after based on the current stock and the quantity change
        if (type == StockMovement.MovementType.PURCHASE || 
            type == StockMovement.MovementType.RETURN || 
            type == StockMovement.MovementType.INVENTORY) {
            // For additions: stockBefore is current - quantity, stockAfter is current
            movement.setStockBefore(product.getStockQuantity() - quantity);
            movement.setStockAfter(product.getStockQuantity());
        } else {
            // For reductions: stockBefore is current + quantity, stockAfter is current
            movement.setStockBefore(product.getStockQuantity() + quantity);
            movement.setStockAfter(product.getStockQuantity());
        }
        
        movement.setReferenceNumber(referenceNumber);
        movement.setReason(reason);
        movement.setMovementDate(LocalDateTime.now());

        return stockMovementRepository.save(movement);
    }
}
