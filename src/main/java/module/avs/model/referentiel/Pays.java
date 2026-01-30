package module.avs.model.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pays")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Pays {
    
    @Id
    @Column(length = 2)
    private String code;
    
    @Column(length = 100, nullable = false)
    private String label;
}
