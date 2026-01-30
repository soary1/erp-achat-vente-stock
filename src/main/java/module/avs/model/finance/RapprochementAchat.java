package module.avs.model.finance;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.stock.BonReception;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rapprochement_achat")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RapprochementAchat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private FactureFournisseur facture;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_id")
    private BonReception reception;
    
    @Column(name = "montant_rapproche", precision = 19, scale = 2, nullable = false)
    private BigDecimal montantRapproche;
    
    @Column(name = "is_match")
    @Builder.Default
    private Boolean isMatch = false;
    
    @Column(columnDefinition = "TEXT")
    private String commentaire;
}
