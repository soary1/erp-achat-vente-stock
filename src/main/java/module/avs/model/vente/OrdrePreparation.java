package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.security.Utilisateur;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordre_preparation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdrePreparation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, unique = true, nullable = false)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private CommandeClient commande;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bon_livraison_id")
    private BonLivraison bonLivraison;
    
    @Column(name = "statut_code", length = 50)
    @Builder.Default
    private String statutCode = "EN_ATTENTE"; // EN_ATTENTE, EN_COURS, TERMINE, ANNULE
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preparateur_id")
    private Utilisateur preparateur;
    
    @Column(name = "date_creation")
    @Builder.Default
    private OffsetDateTime dateCreation = OffsetDateTime.now();
    
    @Column(name = "date_debut_preparation")
    private OffsetDateTime dateDebutPreparation;
    
    @Column(name = "date_fin_preparation")
    private OffsetDateTime dateFinPreparation;
    
    @Column(name = "priorite")
    @Builder.Default
    private Integer priorite = 0;
    
    @OneToMany(mappedBy = "ordre", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneOrdrePreparation> lignes = new ArrayList<>();
    
    public void addLigne(LigneOrdrePreparation ligne) {
        lignes.add(ligne);
        ligne.setOrdre(this);
    }
    
    public boolean isTermine() {
        return lignes.stream().allMatch(l -> 
            l.getQtyPreparee().compareTo(l.getQtyAPreparer()) >= 0
        );
    }
    
    public boolean isEnCours() {
        return lignes.stream().anyMatch(l -> 
            l.getQtyPreparee().compareTo(java.math.BigDecimal.ZERO) > 0
        );
    }
}
