package module.avs.model.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "methode_valorisation")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MethodeValorisation {
    
    @Id
    @Column(length = 20)
    private String code;
    
    @Column(length = 100, nullable = false)
    private String label;
    
    @Column(columnDefinition = "TEXT")
    private String description;
}
