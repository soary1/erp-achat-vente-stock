package module.avs.model.achat;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Site;
import module.avs.model.referentiel.Devise;
import module.avs.model.security.Utilisateur;
import module.avs.model.tiers.Fournisseur;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "commande_achat")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CommandeAchat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_achat_id")
    private DemandeAchat demandeAchat;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acheteur_id")
    private Utilisateur acheteur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_code", nullable = false)
    private Devise devise;
    
    @Column(name = "total_ht", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalHT = BigDecimal.ZERO;
    
    @Column(name = "total_ttc", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalTTC = BigDecimal.ZERO;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    private String statutCode;
    
    @Column(name = "date_commande")
    @Builder.Default
    private LocalDate dateCommande = LocalDate.now();
    
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneCommandeAchat> lignes = new ArrayList<>();
    
    public void addLigne(LigneCommandeAchat ligne) {
        lignes.add(ligne);
        ligne.setCommande(this);
    }
    
    public void removeLigne(LigneCommandeAchat ligne) {
        lignes.remove(ligne);
        ligne.setCommande(null);
    }
    
    public void recalculerTotaux() {
        this.totalHT = lignes.stream()
            .map(l -> l.getUnitPrice().multiply(l.getQtyOrdered()))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        // Calculer TTC avec taxes si nécessaire
        this.totalTTC = this.totalHT;
    }
}
