package com.erp.achats.ventes.service;

import com.erp.achats.ventes.exception.BadRequestException;
import com.erp.achats.ventes.exception.ResourceNotFoundException;
import com.erp.achats.ventes.model.Product;
import com.erp.achats.ventes.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public Product getProductByCode(String code) {
        return productRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with code: " + code));
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    @Transactional
    public Product createProduct(Product product) {
        if (productRepository.findByCode(product.getCode()).isPresent()) {
            throw new BadRequestException("Product code already exists: " + product.getCode());
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);

        if (!product.getCode().equals(productDetails.getCode()) &&
            productRepository.findByCode(productDetails.getCode()).isPresent()) {
            throw new BadRequestException("Product code already exists: " + productDetails.getCode());
        }

        product.setCode(productDetails.getCode());
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPurchasePrice(productDetails.getPurchasePrice());
        product.setSalePrice(productDetails.getSalePrice());
        product.setMinStockLevel(productDetails.getMinStockLevel());
        product.setMaxStockLevel(productDetails.getMaxStockLevel());
        product.setUnit(productDetails.getUnit());
        product.setCategory(productDetails.getCategory());
        product.setActive(productDetails.getActive());

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void updateStock(Long productId, Integer quantityChange) {
        Product product = getProductById(productId);
        int newQuantity = product.getStockQuantity() + quantityChange;
        
        if (newQuantity < 0) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }
        
        product.setStockQuantity(newQuantity);
        productRepository.save(product);
    }
}
