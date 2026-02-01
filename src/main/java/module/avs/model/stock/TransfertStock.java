package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Depot;
import module.avs.model.security.Utilisateur;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transfert_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransfertStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_source_id", nullable = false)
    private Depot depotSource;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_dest_id", nullable = false)
    private Depot depotDest;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    @Builder.Default
    private String statutCode = "DEMANDE";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approbateur_id")
    private Utilisateur approbateur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_id")
    private Utilisateur expediteur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepteur_id")
    private Utilisateur recepteur;
    
    @Column(name = "date_demande")
    @Builder.Default
    private OffsetDateTime dateDemande = OffsetDateTime.now();
    
    @Column(name = "date_approbation")
    private OffsetDateTime dateApprobation;
    
    @Column(name = "date_expedition")
    private OffsetDateTime dateExpedition;
    
    @Column(name = "date_reception")
    private OffsetDateTime dateReception;
    
    @Column(columnDefinition = "TEXT")
    private String motif;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @OneToMany(mappedBy = "transfert", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneTransfertStock> lignes = new ArrayList<>();
    
    public void addLigne(LigneTransfertStock ligne) {
        lignes.add(ligne);
        ligne.setTransfert(this);
    }
}
