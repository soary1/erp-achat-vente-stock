package module.avs.model.security;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "role")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String code;
    
    @Column(length = 100)
    private String label;
}
