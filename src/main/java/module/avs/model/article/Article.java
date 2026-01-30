package module.avs.model.article;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Societe;
import module.avs.model.referentiel.TypeTaxe;
import module.avs.model.referentiel.UniteMesure;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "article")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Article {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "societe_id", nullable = false)
    private Societe societe;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "famille_id", nullable = false)
    private FamilleArticle famille;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_code", nullable = false)
    private UniteMesure unite;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxe_vente_code")
    private TypeTaxe taxeVente;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxe_achat_code")
    private TypeTaxe taxeAchat;
    
    @Column(length = 100, nullable = false)
    private String sku;
    
    @Column(length = 255, nullable = false)
    private String label;
    
    @Column(precision = 10, scale = 3)
    private BigDecimal weight;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
