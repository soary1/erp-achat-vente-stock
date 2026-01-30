package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Depot;
import module.avs.model.stock.Lot;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reservation_stock")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReservationStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_commande_id", nullable = false)
    private LigneCommandeClient ligneCommande;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @Column(name = "qty_reservee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyReservee;
}
