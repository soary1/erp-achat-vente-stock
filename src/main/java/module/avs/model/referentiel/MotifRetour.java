package module.avs.model.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "motif_retour")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MotifRetour {
    
    @Id
    @Column(length = 50)
    private String code;
    
    @Column(nullable = false, length = 100)
    private String label;
}
