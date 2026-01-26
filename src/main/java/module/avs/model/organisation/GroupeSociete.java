package module.avs.model.organisation;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "groupe_societe")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GroupeSociete {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String code;
    
    @Column(length = 200, nullable = false)
    private String name;
}
