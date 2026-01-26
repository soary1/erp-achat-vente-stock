package module.avs.model.organisation;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "site")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Site {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "societe_id", nullable = false)
    private Societe societe;
    
    @Column(length = 50, nullable = false)
    private String code;
    
    @Column(length = 200, nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    // Coordonnées géographiques (latitude, longitude)
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
