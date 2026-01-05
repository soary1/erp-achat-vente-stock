package com.erp.achats.ventes.controller;

import com.erp.achats.ventes.model.Purchase;
import com.erp.achats.ventes.service.PurchaseService;
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
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Purchases", description = "Purchase management APIs")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    @Operation(summary = "Get all purchases")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PURCHASE_AGENT')")
    public ResponseEntity<List<Purchase>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PURCHASE_AGENT')")
    public ResponseEntity<Purchase> getPurchaseById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get purchases by status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PURCHASE_AGENT')")
    public ResponseEntity<List<Purchase>> getPurchasesByStatus(@PathVariable Purchase.PurchaseStatus status) {
        return ResponseEntity.ok(purchaseService.getPurchasesByStatus(status));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get purchases by date range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PURCHASE_AGENT')")
    public ResponseEntity<List<Purchase>> getPurchasesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(purchaseService.getPurchasesByDateRange(startDate, endDate));
    }

    @PostMapping
    @Operation(summary = "Create a new purchase")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PURCHASE_AGENT')")
    public ResponseEntity<Purchase> createPurchase(@Valid @RequestBody Purchase purchase) {
        return new ResponseEntity<>(purchaseService.createPurchase(purchase), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/receive")
    @Operation(summary = "Receive a purchase and update stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PURCHASE_AGENT')")
    public ResponseEntity<Purchase> receivePurchase(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.receivePurchase(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a purchase")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PURCHASE_AGENT')")
    public ResponseEntity<Purchase> cancelPurchase(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.cancelPurchase(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a purchase")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id) {
        purchaseService.deletePurchase(id);
        return ResponseEntity.noContent().build();
    }
}
