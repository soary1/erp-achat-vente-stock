package module.avs.model.achat;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_demande_achat")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LigneDemandeAchat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_achat_id", nullable = false)
    @ToString.Exclude
    private DemandeAchat demandeAchat;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @Column(name = "qty_demandee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyDemandee;
    
    @Column(columnDefinition = "TEXT")
    private String description;
}
