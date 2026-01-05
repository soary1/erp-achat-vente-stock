package com.erp.achats.ventes.service;

import com.erp.achats.ventes.exception.BadRequestException;
import com.erp.achats.ventes.exception.ResourceNotFoundException;
import com.erp.achats.ventes.model.*;
import com.erp.achats.ventes.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final StockMovementService stockMovementService;

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
    }

    public List<Sale> getSalesByStatus(Sale.SaleStatus status) {
        return saleRepository.findByStatus(status);
    }

    public List<Sale> getSalesByDateRange(LocalDate startDate, LocalDate endDate) {
        return saleRepository.findBySaleDateBetween(startDate, endDate);
    }

    @Transactional
    public Sale createSale(Sale sale) {
        if (saleRepository.findByReferenceNumber(sale.getReferenceNumber()).isPresent()) {
            throw new BadRequestException("Sale reference number already exists: " + sale.getReferenceNumber());
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SaleItem item : sale.getItems()) {
            Product product = productService.getProductById(item.getProduct().getId());
            
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }
            
            item.setProduct(product);
            item.setSale(sale);
            
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setTotalPrice(itemTotal);
            totalAmount = totalAmount.add(itemTotal);
        }

        sale.setTotalAmount(totalAmount);
        sale.setStatus(Sale.SaleStatus.PENDING);
        
        return saleRepository.save(sale);
    }

    @Transactional
    public Sale completeSale(Long id) {
        Sale sale = getSaleById(id);

        if (sale.getStatus() != Sale.SaleStatus.PENDING) {
            throw new BadRequestException("Only pending sales can be completed");
        }

        for (SaleItem item : sale.getItems()) {
            productService.updateStock(item.getProduct().getId(), -item.getQuantity());

            stockMovementService.createStockMovement(
                    item.getProduct().getId(),
                    StockMovement.MovementType.SALE,
                    item.getQuantity(),
                    sale.getReferenceNumber(),
                    "Sale completed"
            );
        }

        sale.setStatus(Sale.SaleStatus.COMPLETED);
        return saleRepository.save(sale);
    }

    @Transactional
    public Sale cancelSale(Long id) {
        Sale sale = getSaleById(id);

        if (sale.getStatus() == Sale.SaleStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed sale");
        }

        sale.setStatus(Sale.SaleStatus.CANCELLED);
        return saleRepository.save(sale);
    }

    @Transactional
    public void deleteSale(Long id) {
        Sale sale = getSaleById(id);
        
        if (sale.getStatus() == Sale.SaleStatus.COMPLETED) {
            throw new BadRequestException("Cannot delete a completed sale");
        }
        
        saleRepository.delete(sale);
    }
}
