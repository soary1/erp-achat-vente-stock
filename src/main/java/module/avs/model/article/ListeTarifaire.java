package module.avs.model.article;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.referentiel.Devise;
import java.util.UUID;

@Entity
@Table(name = "liste_tarifaire")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ListeTarifaire {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, unique = true)
    private String code;
    
    @Column(length = 100)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_code", nullable = false)
    private Devise devise;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
