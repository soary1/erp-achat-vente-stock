package com.erp.achats.ventes.service;

import com.erp.achats.ventes.exception.BadRequestException;
import com.erp.achats.ventes.exception.ResourceNotFoundException;
import com.erp.achats.ventes.model.*;
import com.erp.achats.ventes.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductService productService;
    private final StockMovementService stockMovementService;

    public List<Inventory> getAllInventories() {
        return inventoryRepository.findAll();
    }

    public Inventory getInventoryById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
    }

    public List<Inventory> getInventoriesByStatus(Inventory.InventoryStatus status) {
        return inventoryRepository.findByStatus(status);
    }

    @Transactional
    public Inventory createInventory(Inventory inventory) {
        if (inventoryRepository.findByReferenceNumber(inventory.getReferenceNumber()).isPresent()) {
            throw new BadRequestException("Inventory reference number already exists: " + inventory.getReferenceNumber());
        }

        for (InventoryItem item : inventory.getItems()) {
            Product product = productService.getProductById(item.getProduct().getId());
            item.setProduct(product);
            item.setInventory(inventory);
            item.setSystemQuantity(product.getStockQuantity());
            item.setDifference(item.getActualQuantity() - item.getSystemQuantity());
        }

        inventory.setStatus(Inventory.InventoryStatus.IN_PROGRESS);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory completeInventory(Long id) {
        Inventory inventory = getInventoryById(id);

        if (inventory.getStatus() != Inventory.InventoryStatus.IN_PROGRESS) {
            throw new BadRequestException("Only in-progress inventories can be completed");
        }

        for (InventoryItem item : inventory.getItems()) {
            if (item.getDifference() != 0) {
                Product product = item.getProduct();
                int quantityChange = item.getDifference();
                
                productService.updateStock(product.getId(), quantityChange);

                stockMovementService.createStockMovement(
                        product.getId(),
                        StockMovement.MovementType.INVENTORY,
                        Math.abs(quantityChange),
                        inventory.getReferenceNumber(),
                        "Inventory adjustment: " + item.getRemarks()
                );
            }
        }

        inventory.setStatus(Inventory.InventoryStatus.COMPLETED);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory cancelInventory(Long id) {
        Inventory inventory = getInventoryById(id);

        if (inventory.getStatus() == Inventory.InventoryStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed inventory");
        }

        inventory.setStatus(Inventory.InventoryStatus.CANCELLED);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void deleteInventory(Long id) {
        Inventory inventory = getInventoryById(id);
        
        if (inventory.getStatus() == Inventory.InventoryStatus.COMPLETED) {
            throw new BadRequestException("Cannot delete a completed inventory");
        }
        
        inventoryRepository.delete(inventory);
    }
}
