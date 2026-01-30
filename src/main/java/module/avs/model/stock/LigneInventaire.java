package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Emplacement;
import module.avs.model.security.Utilisateur;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ligne_inventaire")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LigneInventaire {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventaire_id", nullable = false)
    private Inventaire inventaire;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_id")
    private Emplacement emplacement;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @Column(name = "qty_theorique", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal qtyTheorique = BigDecimal.ZERO;
    
    @Column(name = "qty_reelle_retenue", precision = 19, scale = 4)
    private BigDecimal qtyReelleRetenue;
    
    @Column(name = "est_traitee")
    @Builder.Default
    private Boolean estTraitee = false;
    
    @Column(name = "est_validee")
    @Builder.Default
    private Boolean estValidee = false;
    
    @Column(name = "notes_arbitrage", columnDefinition = "TEXT")
    private String notesArbitrage;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arbitrage_par")
    private Utilisateur arbitragePar;
    
    @Column(name = "date_arbitrage")
    private OffsetDateTime dateArbitrage;
    
    @OneToMany(mappedBy = "ligneInventaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaisieInventaire> saisies = new ArrayList<>();
    
    public BigDecimal getEcartFinal() {
        if (qtyReelleRetenue == null) return BigDecimal.ZERO;
        return qtyReelleRetenue.subtract(qtyTheorique);
    }
}
