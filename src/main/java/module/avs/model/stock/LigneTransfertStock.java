package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Emplacement;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_transfert_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneTransfertStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfert_id", nullable = false)
    private TransfertStock transfert;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_source_id")
    private Emplacement emplacementSource;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_dest_id")
    private Emplacement emplacementDest;
    
    @Column(name = "qty_demandee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyDemandee;
    
    @Column(name = "qty_expedie", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyExpedie = BigDecimal.ZERO;
    
    @Column(name = "qty_recue", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyRecue = BigDecimal.ZERO;
    
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;
}
