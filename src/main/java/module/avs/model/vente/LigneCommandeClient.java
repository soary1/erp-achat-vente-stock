package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_commande_client")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LigneCommandeClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private CommandeClient commande;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @Column(name = "qty_ordered", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyOrdered;
    
    @Column(name = "qty_delivered", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyDelivered = BigDecimal.ZERO;
    
    @Column(name = "price_unit", precision = 19, scale = 2, nullable = false)
    private BigDecimal priceUnit;
    
    @Column(name = "remise_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal remisePct = BigDecimal.ZERO;
    
    public BigDecimal getMontantHT() {
        BigDecimal montant = priceUnit.multiply(qtyOrdered);
        if (remisePct != null && remisePct.compareTo(BigDecimal.ZERO) > 0) {
            montant = montant.subtract(montant.multiply(remisePct).divide(new BigDecimal("100")));
        }
        return montant;
    }
    
    public BigDecimal getQtyRestante() {
        return qtyOrdered.subtract(qtyDelivered != null ? qtyDelivered : BigDecimal.ZERO);
    }
}
