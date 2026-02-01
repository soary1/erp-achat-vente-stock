package module.avs.model.achat;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Site;
import module.avs.model.referentiel.Devise;
import module.avs.model.security.Utilisateur;
import module.avs.model.tiers.Fournisseur;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demande_achat")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DemandeAchat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    private String statutCode;
    
    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @OneToMany(mappedBy = "demandeAchat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<LigneDemandeAchat> lignes = new ArrayList<>();
    
    public void addLigne(LigneDemandeAchat ligne) {
        lignes.add(ligne);
        ligne.setDemandeAchat(this);
    }
    
    public void removeLigne(LigneDemandeAchat ligne) {
        lignes.remove(ligne);
        ligne.setDemandeAchat(null);
    }
}
