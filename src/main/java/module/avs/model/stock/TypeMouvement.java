package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_mouvement")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TypeMouvement {
    
    @Id
    @Column(length = 50)
    private String code;
    
    @Column(length = 100, nullable = false)
    private String label;
    
    @Column(nullable = false)
    private Integer sens; // 1 = entrée, -1 = sortie, 0 = neutre
}
