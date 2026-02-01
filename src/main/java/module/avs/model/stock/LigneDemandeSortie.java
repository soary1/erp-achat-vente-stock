package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Emplacement;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_demande_sortie")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneDemandeSortie {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeSortieStock demande;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_id")
    private Emplacement emplacement;
    
    @Column(name = "qty_demandee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyDemandee;
    
    @Column(name = "qty_executee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyExecutee = BigDecimal.ZERO;
    
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal montant;
    
    public void calculerMontant() {
        if (qtyDemandee != null && unitCost != null) {
            this.montant = qtyDemandee.multiply(unitCost);
        }
    }
    
    public BigDecimal getMontant() {
        return (qtyDemandee != null && unitCost != null) ? qtyDemandee.multiply(unitCost) : BigDecimal.ZERO;
    }
}
