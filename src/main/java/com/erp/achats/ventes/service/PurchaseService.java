package com.erp.achats.ventes.service;

import com.erp.achats.ventes.exception.BadRequestException;
import com.erp.achats.ventes.exception.ResourceNotFoundException;
import com.erp.achats.ventes.model.*;
import com.erp.achats.ventes.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductService productService;
    private final StockMovementService stockMovementService;

    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    public Purchase getPurchaseById(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));
    }

    public List<Purchase> getPurchasesByStatus(Purchase.PurchaseStatus status) {
        return purchaseRepository.findByStatus(status);
    }

    public List<Purchase> getPurchasesByDateRange(LocalDate startDate, LocalDate endDate) {
        return purchaseRepository.findByPurchaseDateBetween(startDate, endDate);
    }

    @Transactional
    public Purchase createPurchase(Purchase purchase) {
        if (purchaseRepository.findByReferenceNumber(purchase.getReferenceNumber()).isPresent()) {
            throw new BadRequestException("Purchase reference number already exists: " + purchase.getReferenceNumber());
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseItem item : purchase.getItems()) {
            Product product = productService.getProductById(item.getProduct().getId());
            item.setProduct(product);
            item.setPurchase(purchase);
            
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setTotalPrice(itemTotal);
            totalAmount = totalAmount.add(itemTotal);
        }

        purchase.setTotalAmount(totalAmount);
        purchase.setStatus(Purchase.PurchaseStatus.PENDING);
        
        return purchaseRepository.save(purchase);
    }

    @Transactional
    public Purchase receivePurchase(Long id) {
        Purchase purchase = getPurchaseById(id);

        if (purchase.getStatus() != Purchase.PurchaseStatus.PENDING) {
            throw new BadRequestException("Only pending purchases can be received");
        }

        for (PurchaseItem item : purchase.getItems()) {
            productService.updateStock(item.getProduct().getId(), item.getQuantity());

            stockMovementService.createStockMovement(
                    item.getProduct().getId(),
                    StockMovement.MovementType.PURCHASE,
                    item.getQuantity(),
                    purchase.getReferenceNumber(),
                    "Purchase received"
            );
        }

        purchase.setStatus(Purchase.PurchaseStatus.RECEIVED);
        return purchaseRepository.save(purchase);
    }

    @Transactional
    public Purchase cancelPurchase(Long id) {
        Purchase purchase = getPurchaseById(id);

        if (purchase.getStatus() == Purchase.PurchaseStatus.RECEIVED) {
            throw new BadRequestException("Cannot cancel a received purchase");
        }

        purchase.setStatus(Purchase.PurchaseStatus.CANCELLED);
        return purchaseRepository.save(purchase);
    }

    @Transactional
    public void deletePurchase(Long id) {
        Purchase purchase = getPurchaseById(id);
        
        if (purchase.getStatus() == Purchase.PurchaseStatus.RECEIVED) {
            throw new BadRequestException("Cannot delete a received purchase");
        }
        
        purchaseRepository.delete(purchase);
    }
}
