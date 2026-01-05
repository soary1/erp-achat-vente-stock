package com.erp.achats.ventes.repository;

import com.erp.achats.ventes.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    Optional<Sale> findByReferenceNumber(String referenceNumber);
    
    List<Sale> findByStatus(Sale.SaleStatus status);
    
    List<Sale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);
}
