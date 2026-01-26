package module.avs.model.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mode_paiement")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ModePaiement {
    
    @Id
    @Column(length = 50)
    private String code;
    
    @Column(length = 100, nullable = false)
    private String label;
}
