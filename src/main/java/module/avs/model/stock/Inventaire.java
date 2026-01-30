package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Site;
import module.avs.model.security.Utilisateur;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventaire")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Inventaire {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;
    
    @Column(length = 200)
    private String description;
    
    @Column(name = "type_code", length = 50)
    private String typeCode; // ANNUEL, TOURNANT, SPOT
    
    @Column(name = "statut_code", length = 50, nullable = false)
    private String statutCode;
    
    @Column(name = "date_planification")
    private LocalDate datePlanification;
    
    @Column(name = "date_debut")
    private OffsetDateTime dateDebut;
    
    @Column(name = "date_cloture")
    private OffsetDateTime dateCloture;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar;
    
    @OneToMany(mappedBy = "inventaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneInventaire> lignes = new ArrayList<>();
    
    public void addLigne(LigneInventaire ligne) {
        lignes.add(ligne);
        ligne.setInventaire(this);
    }
}
