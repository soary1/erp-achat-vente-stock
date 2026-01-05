package com.erp.achats.ventes.repository;

import com.erp.achats.ventes.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByCode(String code);
    
    List<Product> findByActiveTrue();
    
    List<Product> findByCategory(String category);
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.minStockLevel AND p.active = true")
    List<Product> findLowStockProducts();
}
