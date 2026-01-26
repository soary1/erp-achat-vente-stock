package module.avs.model.security;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_audit")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class JournalAudit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "entity_name", length = 100, nullable = false)
    private String entityName;
    
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    @Column(length = 50, nullable = false)
    private String action;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
    
    @Column(columnDefinition = "jsonb")
    private String changes;
    
    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
