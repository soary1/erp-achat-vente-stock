package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Emplacement;
import module.avs.model.security.Utilisateur;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ajustement_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjustementStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_id")
    private Emplacement emplacement;
    
    @Column(name = "qty_theorique", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyTheorique;
    
    @Column(name = "qty_reelle", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyReelle;
    
    // qty_ecart est calculé automatiquement par la base (GENERATED ALWAYS AS)
    @Column(name = "qty_ecart", precision = 19, scale = 4, insertable = false, updatable = false)
    private BigDecimal qtyEcart;
    
    @Column(name = "motif_code", length = 50, nullable = false)
    private String motifCode;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    @Builder.Default
    private String statutCode = "BROUILLON";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approbateur_niveau1_id")
    private Utilisateur approbateurNiveau1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approbateur_final_id")
    private Utilisateur approbateurFinal;
    
    @Column(name = "date_demande")
    @Builder.Default
    private OffsetDateTime dateDemande = OffsetDateTime.now();
    
    @Column(name = "date_approbation_niveau1")
    private OffsetDateTime dateApprobationNiveau1;
    
    @Column(name = "date_approbation_finale")
    private OffsetDateTime dateApprobationFinale;
    
    @Column(name = "date_execution")
    private OffsetDateTime dateExecution;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String justification;
    
    @Column(name = "commentaire_approbation", columnDefinition = "TEXT")
    private String commentaireApprobation;
    
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;
    
    @Column(name = "montant_impact", precision = 19, scale = 2)
    private BigDecimal montantImpact;
    
    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;
    
    public void calculerMontantImpact() {
        if (qtyEcart != null && unitCost != null) {
            this.montantImpact = qtyEcart.multiply(unitCost);
        }
    }
}
