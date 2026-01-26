package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.security.Utilisateur;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "controle_qualite")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ControleQualite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_reception_id", nullable = false)
    private LigneBonReception ligneReception;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controleur_id", nullable = false)
    private Utilisateur controleur;
    
    @Column(name = "qty_inspectee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyInspectee;
    
    @Column(name = "qty_acceptee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyAcceptee;
    
    @Column(name = "qty_rejetee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyRejetee;
    
    @Column(name = "motif_rejet_code", length = 50)
    private String motifRejetCode;
    
    @Column(name = "photo_preuve_url", columnDefinition = "TEXT")
    private String photoPreuveUrl;
    
    @Column(columnDefinition = "TEXT")
    private String commentaires;
    
    @Column(name = "date_controle")
    @Builder.Default
    private OffsetDateTime dateControle = OffsetDateTime.now();
}
