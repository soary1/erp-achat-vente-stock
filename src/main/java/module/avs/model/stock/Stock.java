package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Emplacement;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Stock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_id")
    private Emplacement emplacement;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @Column(name = "qty_reel", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyReel = BigDecimal.ZERO;
    
    @Column(name = "qty_reserve", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyReserve = BigDecimal.ZERO;
    
    @Version
    private Long version;
    
    public BigDecimal getQtyDisponible() {
        return qtyReel.subtract(qtyReserve);
    }
}
