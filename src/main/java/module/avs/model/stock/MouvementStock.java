package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Emplacement;
import module.avs.model.security.Utilisateur;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mouvement_stock")
@Immutable // CRITIQUE : Les mouvements ne peuvent pas être modifiés
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MouvementStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, unique = true, nullable = false)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_mouvement_code", nullable = false)
    private TypeMouvement typeMouvement;
    
    @Column(name = "reference_doc", length = 100)
    private String referenceDoc;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_source_id")
    private Depot depotSource;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_source_id")
    private Emplacement emplacementSource;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_dest_id")
    private Depot depotDest;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emplacement_dest_id")
    private Emplacement emplacementDest;
    
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal qty;
    
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
    
    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
