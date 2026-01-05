package com.erp.achats.ventes.controller;

import com.erp.achats.ventes.dto.LoginRequest;
import com.erp.achats.ventes.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAuthenticationAndProductCreation() throws Exception {
        // Test login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Test accessing protected endpoint without token
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());

        // Test accessing protected endpoint with token
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Test creating a product
        Product product = new Product();
        product.setCode("PROD001");
        product.setName("Test Product");
        product.setDescription("A test product");
        product.setPurchasePrice(new BigDecimal("100.00"));
        product.setSalePrice(new BigDecimal("150.00"));
        product.setStockQuantity(10);
        product.setMinStockLevel(5);
        product.setMaxStockLevel(100);
        product.setUnit("piece");
        product.setCategory("Test Category");
        product.setActive(true);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PROD001"))
                .andExpect(jsonPath("$.name").value("Test Product"));

        // Test retrieving the product
        mockMvc.perform(get("/api/products/code/PROD001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROD001"))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }
}
