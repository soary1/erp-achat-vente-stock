package com.erp.achats.ventes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleName name;

    @Column(length = 255)
    private String description;

    public enum RoleName {
        ROLE_ADMIN,
        ROLE_MANAGER,
        ROLE_PURCHASE_AGENT,
        ROLE_SALES_AGENT,
        ROLE_INVENTORY_MANAGER,
        ROLE_USER
    }
}
