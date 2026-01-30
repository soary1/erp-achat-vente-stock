package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_devis_client")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LigneDevisClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devis_id", nullable = false)
    private DevisClient devis;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal qty;
    
    @Column(name = "price_unit", precision = 19, scale = 2, nullable = false)
    private BigDecimal priceUnit;
    
    @Column(name = "remise_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal remisePct = BigDecimal.ZERO;
    
    public BigDecimal getMontantHT() {
        BigDecimal montant = priceUnit.multiply(qty);
        if (remisePct != null && remisePct.compareTo(BigDecimal.ZERO) > 0) {
            montant = montant.subtract(montant.multiply(remisePct).divide(new BigDecimal("100")));
        }
        return montant;
    }
}
