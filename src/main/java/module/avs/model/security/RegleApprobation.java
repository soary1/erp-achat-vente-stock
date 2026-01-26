package module.avs.model.security;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Societe;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "regle_approbation")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RegleApprobation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "societe_id")
    private Societe societe;
    
    @Column(name = "document_type", length = 50, nullable = false)
    private String documentType;
    
    @Column(name = "min_amount", precision = 19, scale = 2)
    private BigDecimal minAmount;
    
    @Column(name = "max_amount", precision = 19, scale = 2)
    private BigDecimal maxAmount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @Column(name = "level_index", nullable = false)
    private Integer levelIndex;
}
