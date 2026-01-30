package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Emplacement;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_bon_reception")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LigneBonReception {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bon_reception_id", nullable = false)
    private BonReception bonReception;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_id")
    private Emplacement emplacement;
    
    @Column(name = "qty_received", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyReceived;
    
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;
}
