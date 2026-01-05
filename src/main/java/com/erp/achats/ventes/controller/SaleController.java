package com.erp.achats.ventes.controller;

import com.erp.achats.ventes.model.Sale;
import com.erp.achats.ventes.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sales", description = "Sale management APIs")
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    @Operation(summary = "Get all sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_AGENT')")
    public ResponseEntity<List<Sale>> getAllSales() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sale by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_AGENT')")
    public ResponseEntity<Sale> getSaleById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get sales by status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_AGENT')")
    public ResponseEntity<List<Sale>> getSalesByStatus(@PathVariable Sale.SaleStatus status) {
        return ResponseEntity.ok(saleService.getSalesByStatus(status));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get sales by date range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_AGENT')")
    public ResponseEntity<List<Sale>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(saleService.getSalesByDateRange(startDate, endDate));
    }

    @PostMapping
    @Operation(summary = "Create a new sale")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_AGENT')")
    public ResponseEntity<Sale> createSale(@Valid @RequestBody Sale sale) {
        return new ResponseEntity<>(saleService.createSale(sale), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete a sale and update stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_AGENT')")
    public ResponseEntity<Sale> completeSale(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.completeSale(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a sale")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_AGENT')")
    public ResponseEntity<Sale> cancelSale(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.cancelSale(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a sale")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
        return ResponseEntity.noContent().build();
    }
}
