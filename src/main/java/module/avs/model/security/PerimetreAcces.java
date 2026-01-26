package module.avs.model.security;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Site;
import module.avs.model.organisation.Societe;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "perimetre_acces")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PerimetreAcces {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "societe_id")
    private Societe societe;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;
    
    @Column(name = "max_amount_approval", precision = 19, scale = 2)
    private BigDecimal maxAmountApproval;
    
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;
}
