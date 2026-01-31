package module.avs.model.tiers;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.referentiel.Devise;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "fournisseur")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Fournisseur {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String code;
    
    @Column(length = 200, nullable = false)
    private String name;
    
    @Column(name = "tax_id", length = 50)
    private String taxId;
    
    @Column(length = 200)
    private String email;
    
    @Column(length = 50)
    private String telephone;
    
    @Column(columnDefinition = "TEXT")
    private String adresse;
    
    // Coordonnées géographiques (latitude, longitude)
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_code")
    @ToString.Exclude
    private Devise devise;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
