package module.avs.model.article;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ligne_liste_tarifaire")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LigneListeTarifaire {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liste_tarifaire_id", nullable = false)
    private ListeTarifaire listeTarifaire;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal price;
    
    @Column(name = "min_qty", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minQty = BigDecimal.ONE;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
}
