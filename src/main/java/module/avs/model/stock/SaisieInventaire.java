package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.security.Utilisateur;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saisie_inventaire")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SaisieInventaire {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_inventaire_id", nullable = false)
    private LigneInventaire ligneInventaire;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operateur_id", nullable = false)
    private Utilisateur operateur;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superviseur_id")
    private Utilisateur superviseur;
    
    @Column(name = "qty_comptee", precision = 19, scale = 4, nullable = false)
    private BigDecimal qtyComptee;
    
    @Column(name = "date_saisie")
    @Builder.Default
    private OffsetDateTime dateSaisie = OffsetDateTime.now();
    
    @Column(name = "tour_comptage")
    @Builder.Default
    private Integer tourComptage = 1;
    
    @Column(name = "est_retenue")
    @Builder.Default
    private Boolean estRetenue = false;
}
