package com.erp.achats.ventes.controller;

import com.erp.achats.ventes.model.StockMovement;
import com.erp.achats.ventes.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Stock Movements", description = "Stock movement tracking APIs")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @GetMapping
    @Operation(summary = "Get all stock movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<List<StockMovement>> getAllStockMovements() {
        return ResponseEntity.ok(stockMovementService.getAllStockMovements());
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get stock movements by product")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<List<StockMovement>> getStockMovementsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByProduct(productId));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get stock movements by date range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<List<StockMovement>> getStockMovementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByDateRange(startDate, endDate));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get stock movements by type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<List<StockMovement>> getStockMovementsByType(@PathVariable StockMovement.MovementType type) {
        return ResponseEntity.ok(stockMovementService.getStockMovementsByType(type));
    }
}
