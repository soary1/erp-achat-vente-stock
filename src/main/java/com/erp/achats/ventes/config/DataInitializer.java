package com.erp.achats.ventes.config;

import com.erp.achats.ventes.model.Role;
import com.erp.achats.ventes.model.User;
import com.erp.achats.ventes.repository.RoleRepository;
import com.erp.achats.ventes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Initialize roles
        if (roleRepository.count() == 0) {
            for (Role.RoleName roleName : Role.RoleName.values()) {
                Role role = new Role();
                role.setName(roleName);
                role.setDescription(getDescriptionForRole(roleName));
                roleRepository.save(role);
            }
        }

        // Create default admin user
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@erp.com");
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setActive(true);
            admin.setRoles(Set.of(adminRole));
            
            userRepository.save(admin);
        }
    }

    private String getDescriptionForRole(Role.RoleName roleName) {
        return switch (roleName) {
            case ROLE_ADMIN -> "Administrateur avec accès complet";
            case ROLE_MANAGER -> "Gestionnaire avec accès étendu";
            case ROLE_PURCHASE_AGENT -> "Agent d'achat";
            case ROLE_SALES_AGENT -> "Agent de vente";
            case ROLE_INVENTORY_MANAGER -> "Gestionnaire d'inventaire";
            case ROLE_USER -> "Utilisateur standard";
        };
    }
}
