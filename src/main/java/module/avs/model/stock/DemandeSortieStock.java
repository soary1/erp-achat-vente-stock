package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Depot;
import module.avs.model.security.Utilisateur;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demande_sortie_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeSortieStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
    
    @Column(length = 50, nullable = false)
    private String type; // CONSOMMATION ou REBUT
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;
    
    @Column(name = "motif_code", length = 50, nullable = false)
    private String motifCode;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    @Builder.Default
    private String statutCode = "BROUILLON";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approbateur_id")
    private Utilisateur approbateur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executeur_id")
    private Utilisateur executeur;
    
    @Column(name = "date_demande")
    @Builder.Default
    private OffsetDateTime dateDemande = OffsetDateTime.now();
    
    @Column(name = "date_approbation")
    private OffsetDateTime dateApprobation;
    
    @Column(name = "date_execution")
    private OffsetDateTime dateExecution;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String justification;
    
    @Column(name = "commentaire_approbation", columnDefinition = "TEXT")
    private String commentaireApprobation;
    
    @Column(name = "cout_total", precision = 19, scale = 2)
    private BigDecimal coutTotal;
    
    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneDemandeSortie> lignes = new ArrayList<>();
    
    public void addLigne(LigneDemandeSortie ligne) {
        lignes.add(ligne);
        ligne.setDemande(this);
    }
    
    public void calculerCoutTotal() {
        this.coutTotal = lignes.stream()
            .map(ligne -> ligne.getMontant() != null ? ligne.getMontant() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
