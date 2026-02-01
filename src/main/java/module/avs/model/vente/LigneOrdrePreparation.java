package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Emplacement;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.Lot;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ligne_ordre_preparation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneOrdrePreparation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordre_id", nullable = false)
    private OrdrePreparation ordre;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_commande_id", nullable = false)
    private LigneCommandeClient ligneCommande;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot; // Lot alloué par FIFO
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_id")
    private Emplacement emplacement;
    
    @Column(name = "qty_a_preparer", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyAPreparer;
    
    @Column(name = "qty_preparee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal qtyPreparee = BigDecimal.ZERO;
    
    @Column(name = "date_scan")
    private OffsetDateTime dateScan;
    
    @Column(name = "scanne")
    @Builder.Default
    private Boolean scanne = false;
    
    @Column(name = "forcage_fifo")
    @Builder.Default
    private Boolean forcageFifo = false; // True si l'utilisateur a forcé un lot différent
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forcage_validateur_id")
    private Utilisateur forcageValidateur;
    
    @Column(name = "forcage_motif")
    private String forcageMotif;
    
    public boolean isPret() {
        return qtyPreparee.compareTo(qtyAPreparer) >= 0;
    }
}
