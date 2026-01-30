package module.avs.model.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "devise")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Devise {
    
    @Id
    @Column(length = 3)
    private String code;
    
    @Column(length = 100, nullable = false)
    private String label;
    
    @Column(length = 5)
    private String symbol;
}
