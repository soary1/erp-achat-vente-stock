package module.avs.model.achat;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.referentiel.TypeTaxe;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    
    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal unitPrice;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ligne_commande_achat_taxe",
        joinColumns = @JoinColumn(name = "ligne_id"),
        inverseJoinColumns = @JoinColumn(name = "taxe_code")
    )
    @Builder.Default
    private List<TypeTaxe> taxes = new ArrayList<>();
    
    public BigDecimal getMontantHT() {
        return (unitPrice != null && qtyOrdered != null) ? unitPrice.multiply(qtyOrdered) : BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return "LigneCommandeAchat{" +
                "id=" + id +
                ", article=" + (article != null ? article.getSku() : null) +
                ", qtyOrdered=" + qtyOrdered +
                ", unitPrice=" + unitPrice +
                ", taxes=" + (taxes != null ? taxes.size() : "0") +
                '}';
    }
}
