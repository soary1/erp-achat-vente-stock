package com.erp.achats.ventes.controller;

import com.erp.achats.ventes.model.Inventory;
import com.erp.achats.ventes.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inventories", description = "Inventory management APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Get all inventories")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<List<Inventory>> getAllInventories() {
        return ResponseEntity.ok(inventoryService.getAllInventories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<Inventory> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get inventories by status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<List<Inventory>> getInventoriesByStatus(@PathVariable Inventory.InventoryStatus status) {
        return ResponseEntity.ok(inventoryService.getInventoriesByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<Inventory> createInventory(@Valid @RequestBody Inventory inventory) {
        return new ResponseEntity<>(inventoryService.createInventory(inventory), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete an inventory and adjust stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<Inventory> completeInventory(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.completeInventory(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<Inventory> cancelInventory(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.cancelInventory(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }
}
