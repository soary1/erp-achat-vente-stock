package module.avs.model.organisation;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "depot")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Depot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
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
