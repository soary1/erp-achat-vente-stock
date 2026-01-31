package module.avs.model.achat;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.referentiel.TypeTaxe;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_commande_achat")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LigneCommandeAchat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private CommandeAchat commande;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    @NotNull
    private Article article;
    
    @Column(name = "qty_ordered", precision = 19, scale = 4, nullable = false)
    @NotNull
    @DecimalMin(value = "0.01", inclusive = false)
    private BigDecimal qtyOrdered;
    
    @Column(name = "qty_received", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyReceived = BigDecimal.ZERO;
    
    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal unitPrice;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxe_code")
    private TypeTaxe taxe;
    
    public BigDecimal getMontantHT() {
        return (unitPrice != null && qtyOrdered != null) ? unitPrice.multiply(qtyOrdered) : BigDecimal.ZERO;
    }
    
    public BigDecimal getQtyRestante() {
        return qtyOrdered.subtract(qtyReceived != null ? qtyReceived : BigDecimal.ZERO);
    }

    @Override
    public String toString() {
        return "LigneCommandeAchat{" +
                "id=" + id +
                ", article=" + (article != null ? article.getSku() : null) +
                ", qtyOrdered=" + qtyOrdered +
                ", qtyReceived=" + qtyReceived +
                ", unitPrice=" + unitPrice +
                ", taxe=" + (taxe != null ? taxe.getCode() : null) +
                '}';
    }
}
