package module.avs.model.security;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "utilisateur")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Utilisateur {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 100, nullable = false, unique = true)
    private String username;
    
    @Column(length = 200, nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "utilisateur_role",
        joinColumns = @JoinColumn(name = "utilisateur_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    // Helper method pour Spring Security
    public String getFullName() {
        return username;
    }
}
