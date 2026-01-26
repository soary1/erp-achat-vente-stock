package module.avs.model.article;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.referentiel.MethodeValorisation;
import java.util.UUID;

@Entity
@Table(name = "famille_article")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FamilleArticle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String code;
    
    @Column(length = 200, nullable = false)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "methode_valorisation_code", nullable = false)
    private MethodeValorisation methodeValorisation;
    
    @Column(name = "is_lot_obligatoire")
    @Builder.Default
    private Boolean isLotObligatoire = false;
    
    @Column(name = "is_peremption_obligatoire")
    @Builder.Default
    private Boolean isPeremptionObligatoire = false;
}
