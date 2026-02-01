package module.avs.model.finance;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.security.Utilisateur;
import module.avs.model.tiers.Client;
import module.avs.model.vente.BonLivraison;
import module.avs.model.vente.CommandeClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "facture_client")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FactureClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private CommandeClient commande;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bon_livraison_id")
    private BonLivraison bonLivraison;
    
    @Column(name = "montant_ht", precision = 19, scale = 2, nullable = false)
    private BigDecimal montantHT;
    
    @Column(name = "montant_ttc", precision = 19, scale = 2, nullable = false)
    private BigDecimal montantTTC;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    private String statutCode;
    
    @Column(name = "date_facture", nullable = false)
    private LocalDate dateFacture;
    
    @Column(name = "date_echeance")
    private LocalDate dateEcheance;
    
    @Column(name = "montant_encaisse", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal montantEncaisse = BigDecimal.ZERO;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createur_id")
    private Utilisateur createur;
    
    @Column(name = "date_creation")
    @Builder.Default
    private OffsetDateTime dateCreation = OffsetDateTime.now();
    
    public BigDecimal getMontantRestant() {
        return montantTTC.subtract(montantEncaisse != null ? montantEncaisse : BigDecimal.ZERO);
    }
}
