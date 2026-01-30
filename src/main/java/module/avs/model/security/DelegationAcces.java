package module.avs.model.security;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delegation_acces")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DelegationAcces {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donneur_id", nullable = false)
    private Utilisateur donneur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receveur_id", nullable = false)
    private Utilisateur receveur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
}
