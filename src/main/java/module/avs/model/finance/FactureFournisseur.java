package module.avs.model.finance;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.achat.CommandeAchat;
import module.avs.model.referentiel.Devise;
import module.avs.model.tiers.Fournisseur;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "facture_fournisseur")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FactureFournisseur {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "ref_interne", length = 50, unique = true)
    private String refInterne;
    
    @Column(name = "ref_fournisseur", length = 100)
    private String refFournisseur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_achat_id")
    private CommandeAchat commandeAchat;
    
    @Column(name = "montant_ht", precision = 19, scale = 2, nullable = false)
    private BigDecimal montantHT;
    
    @Column(name = "montant_ttc", precision = 19, scale = 2, nullable = false)
    private BigDecimal montantTTC;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_code", nullable = false)
    private Devise devise;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    private String statutCode;
    
    @Column(name = "date_facture", nullable = false)
    private LocalDate dateFacture;
    
    @Column(name = "date_echeance")
    private LocalDate dateEcheance;
    
    @Column(name = "montant_paye", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal montantPaye = BigDecimal.ZERO;
    
    public BigDecimal getMontantRestant() {
        return montantTTC.subtract(montantPaye != null ? montantPaye : BigDecimal.ZERO);
    }
}
