package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.security.Utilisateur;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bon_livraison")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BonLivraison {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private CommandeClient commande;
    
    @Column(name = "statut_code", length = 50)
    @Builder.Default
    private String statutCode = "BROUILLON";
    
    @Column(name = "date_expedition")
    private OffsetDateTime dateExpedition;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preparateur_id")
    private Utilisateur preparateur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validateur_id")
    private Utilisateur validateur;
    
    @Column(name = "date_preparation")
    private OffsetDateTime datePreparation;
    
    @Column(name = "date_validation")
    private OffsetDateTime dateValidation;
    
    @OneToMany(mappedBy = "bonLivraison", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneBonLivraison> lignes = new ArrayList<>();
    
    public void addLigne(LigneBonLivraison ligne) {
        lignes.add(ligne);
        ligne.setBonLivraison(this);
    }
}
