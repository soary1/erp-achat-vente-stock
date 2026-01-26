package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.stock.Lot;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_bon_livraison")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LigneBonLivraison {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livraison_id", nullable = false)
    private BonLivraison bonLivraison;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @Column(name = "qty_livree", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyLivree;
}
