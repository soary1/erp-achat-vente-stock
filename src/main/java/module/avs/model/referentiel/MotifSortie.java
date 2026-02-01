package module.avs.model.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "motif_sortie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MotifSortie {
    
    @Id
    @Column(length = 50)
    private String code;
    
    @Column(nullable = false, length = 100)
    private String label;
    
    @Column(nullable = false, length = 50)
    private String type; // 'CONSOMMATION' ou 'REBUT'
}
