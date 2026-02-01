package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Site;
import module.avs.model.security.Utilisateur;
import module.avs.model.tiers.Client;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "commande_client")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CommandeClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devis_id")
    private DevisClient devis;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_id")
    private Utilisateur commercial;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validateur_id")
    private Utilisateur validateur;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    private String statutCode;
    
    @Column(name = "total_ht", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalHT = BigDecimal.ZERO;
    
    @Column(name = "total_ttc", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalTTC = BigDecimal.ZERO;
    
    @Column(name = "remise_globale_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal remiseGlobalePct = BigDecimal.ZERO;
    
    @Column(name = "date_validation")
    private OffsetDateTime dateValidation;
    
    @Column(name = "motif_refus")
    private String motifRefus;
    
    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "notes")
    private String notes;
    
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneCommandeClient> lignes = new ArrayList<>();
    
    public void addLigne(LigneCommandeClient ligne) {
        lignes.add(ligne);
        ligne.setCommande(this);
    }
    
    public void recalculerTotaux() {
        this.totalHT = lignes.stream()
            .map(LigneCommandeClient::getMontantHT)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Appliquer la remise globale si présente
        if (remiseGlobalePct != null && remiseGlobalePct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remiseGlobale = this.totalHT.multiply(remiseGlobalePct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            this.totalHT = this.totalHT.subtract(remiseGlobale);
        }
        
        this.totalTTC = this.totalHT; // TODO: ajouter calcul TVA si nécessaire
    }
}
