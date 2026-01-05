package com.erp.achats.ventes.repository;

import com.erp.achats.ventes.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    Optional<Purchase> findByReferenceNumber(String referenceNumber);
    
    List<Purchase> findByStatus(Purchase.PurchaseStatus status);
    
    List<Purchase> findByPurchaseDateBetween(LocalDate startDate, LocalDate endDate);
}
