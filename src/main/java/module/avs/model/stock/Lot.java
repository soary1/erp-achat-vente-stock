package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.article.Article;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lot")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Lot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @Column(name = "numero_lot", length = 100, nullable = false)
    private String numeroLot;
    
    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;
    
    @Column(name = "date_fabrication")
    private LocalDate dateFabrication;
    
    @Column(name = "date_peremption")
    private LocalDate datePeremption;
    
    @Column(name = "statut_qualite_code", length = 50, nullable = false)
    private String statutQualiteCode;
}
