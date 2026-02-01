package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Site;
import module.avs.model.tiers.Client;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "devis_client")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DevisClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_id")
    private module.avs.model.security.Utilisateur commercial;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validateur_id")
    private module.avs.model.security.Utilisateur validateur;
    
    @Column(name = "statut_code", length = 50)
    @Builder.Default
    private String statutCode = "BROUILLON";
    
    @Column(name = "total_ht", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalHT = BigDecimal.ZERO;
    
    @Column(name = "total_ttc", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalTTC = BigDecimal.ZERO;
    
    @Column(name = "remise_globale_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal remiseGlobalePct = BigDecimal.ZERO;
    
    @Column(name = "date_validite")
    private java.time.LocalDate dateValidite;
    
    @Column(name = "date_validation")
    private OffsetDateTime dateValidation;
    
    @Column(name = "motif_refus")
    private String motifRefus;
    
    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "notes")
    private String notes;
    
    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneDevisClient> lignes = new ArrayList<>();
    
    public void addLigne(LigneDevisClient ligne) {
        lignes.add(ligne);
        ligne.setDevis(this);
    }
    
    public void recalculerTotaux() {
        this.totalHT = lignes.stream()
            .map(LigneDevisClient::getMontantHT)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Appliquer la remise globale si présente
        if (remiseGlobalePct != null && remiseGlobalePct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remiseGlobale = this.totalHT.multiply(remiseGlobalePct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            this.totalHT = this.totalHT.subtract(remiseGlobale);
        }
        
        this.totalTTC = this.totalHT; // TODO: ajouter calcul TVA si nécessaire
    }
    
    public BigDecimal getRemiseTotale() {
        BigDecimal remiseLignes = lignes.stream()
            .map(l -> {
                BigDecimal montantBrut = l.getPriceUnit().multiply(l.getQty());
                return montantBrut.subtract(l.getMontantHT());
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal montantAvantRemiseGlobale = lignes.stream()
            .map(LigneDevisClient::getMontantHT)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal remiseGlobale = BigDecimal.ZERO;
        if (remiseGlobalePct != null && remiseGlobalePct.compareTo(BigDecimal.ZERO) > 0) {
            remiseGlobale = montantAvantRemiseGlobale.multiply(remiseGlobalePct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }
        
        return remiseLignes.add(remiseGlobale);
    }
}
