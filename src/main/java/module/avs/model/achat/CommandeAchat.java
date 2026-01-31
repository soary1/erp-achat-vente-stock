package module.avs.model.achat;

import jakarta.persistence.*;
import jakarta.validation.Valid;
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
    @Valid
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
            .map(l -> (l.getUnitPrice() != null && l.getQtyOrdered() != null) ? l.getUnitPrice().multiply(l.getQtyOrdered()) : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            
        this.totalTTC = lignes.stream()
            .map(l -> {
                BigDecimal ht = (l.getUnitPrice() != null && l.getQtyOrdered() != null) ? l.getUnitPrice().multiply(l.getQtyOrdered()) : BigDecimal.ZERO;
                if (l.getTaxes() != null && !l.getTaxes().isEmpty()) {
                    for (module.avs.model.referentiel.TypeTaxe t : l.getTaxes()) {
                        if (t.getRate() != null) {
                             ht = ht.add(ht.multiply(t.getRate()));
                        }
                    }
                }
                return ht;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return "CommandeAchat{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", fournisseur=" + (fournisseur != null ? fournisseur.getName() : null) +
                ", site=" + (site != null ? site.getName() : null) +
                ", acheteur=" + (acheteur != null ? acheteur.getUsername() : null) +
                ", devise=" + (devise != null ? devise.getCode() : null) +
                ", totalHT=" + totalHT +
                ", totalTTC=" + totalTTC +
                ", statutCode='" + statutCode + '\'' +
                ", dateCommande=" + dateCommande +
                '}';
    }
}
