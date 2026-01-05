package com.erp.achats.ventes.repository;

import com.erp.achats.ventes.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    Optional<Inventory> findByReferenceNumber(String referenceNumber);
    
    List<Inventory> findByStatus(Inventory.InventoryStatus status);
}
