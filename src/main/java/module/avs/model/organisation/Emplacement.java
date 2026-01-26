package module.avs.model.organisation;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "emplacement")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Emplacement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;
    
    @Column(length = 50, nullable = false)
    private String code;
    
    @Column(length = 20)
    private String aisle;
    
    @Column(length = 20)
    private String rack;
    
    @Column(length = 20)
    private String shelf;
}
