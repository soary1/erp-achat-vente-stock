package module.avs.model.organisation;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.referentiel.Devise;
import module.avs.model.referentiel.Pays;
import java.util.UUID;

@Entity
@Table(name = "societe")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Societe {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupe_id", nullable = false)
    private GroupeSociete groupe;
    
    @Column(length = 50, nullable = false, unique = true)
    private String code;
    
    @Column(length = 200, nullable = false)
    private String name;
    
    @Column(name = "tax_id", length = 50)
    private String taxId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pays_code")
    private Pays pays;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devise_code", nullable = false)
    private Devise devise;
}
