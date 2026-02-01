package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Emplacement;
import module.avs.model.stock.Lot;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ligne_retour_client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneRetourClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retour_id", nullable = false)
    private RetourClient retour;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_livraison_id")
    private LigneBonLivraison ligneLivraison;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @Column(name = "qty_retournee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyRetournee;
    
    @Column(name = "qty_acceptee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyAcceptee = BigDecimal.ZERO;
    
    @Column(name = "qty_rejetee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyRejetee = BigDecimal.ZERO;
    
    @Column(name = "etat_marchandise", length = 50)
    private String etatMarchandise; // INTACT, ABIME, DEFECTUEUX
    
    @Column(length = 50)
    private String decision; // REINTEGRER, REBUTER, QUARANTAINE
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_id")
    private Emplacement emplacement;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
}
