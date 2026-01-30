package module.avs.model.security;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historique_workflow")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class HistoriqueWorkflow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "document_type", length = 50, nullable = false)
    private String documentType;
    
    @Column(name = "document_id", nullable = false)
    private UUID documentId;
    
    @Column(name = "etape_precedente", length = 50)
    private String etapePrecedente;
    
    @Column(name = "etape_nouvelle", length = 50, nullable = false)
    private String etapeNouvelle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acteur_id", nullable = false)
    private Utilisateur acteur;
    
    @Column(length = 50, nullable = false)
    private String action;
    
    @Column(columnDefinition = "TEXT")
    private String commentaire;
    
    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
