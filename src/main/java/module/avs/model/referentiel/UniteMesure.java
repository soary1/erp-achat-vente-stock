package module.avs.model.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unite_mesure")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UniteMesure {
    
    @Id
    @Column(length = 20)
    private String code;
    
    @Column(length = 100, nullable = false)
    private String label;
}
