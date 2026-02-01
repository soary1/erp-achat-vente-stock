package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Depot;
import module.avs.model.security.Utilisateur;
import module.avs.model.tiers.Client;
import module.avs.model.finance.FactureClient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "retour_client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetourClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private CommandeClient commande;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    private FactureClient facture;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bon_livraison_id")
    private BonLivraison bonLivraison;
    
    @Column(name = "motif_code", length = 50, nullable = false)
    private String motifCode;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    @Builder.Default
    private String statutCode = "DEMANDE";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id")
    private Utilisateur demandeur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approbateur_id")
    private Utilisateur approbateur;
    
    @Column(name = "date_demande")
    @Builder.Default
    private OffsetDateTime dateDemande = OffsetDateTime.now();
    
    @Column(name = "date_approbation")
    private OffsetDateTime dateApprobation;
    
    @Column(name = "date_reception")
    private OffsetDateTime dateReception;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_retour_id")
    private Depot depotRetour;
    
    @Column(name = "montant_rembourse", precision = 19, scale = 2)
    private BigDecimal montantRembourse;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @OneToMany(mappedBy = "retour", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneRetourClient> lignes = new ArrayList<>();
    
    public void addLigne(LigneRetourClient ligne) {
        lignes.add(ligne);
        ligne.setRetour(this);
    }
}
