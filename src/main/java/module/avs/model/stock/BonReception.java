package module.avs.model.stock;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.achat.CommandeAchat;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Site;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bon_reception")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BonReception {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_achat_id")
    private CommandeAchat commandeAchat;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;
    
    @Column(name = "statut_code", length = 50, nullable = false)
    private String statutCode;
    
    @Column(name = "date_reception")
    @Builder.Default
    private OffsetDateTime dateReception = OffsetDateTime.now();
    
    @OneToMany(mappedBy = "bonReception", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneBonReception> lignes = new ArrayList<>();
    
    public void addLigne(LigneBonReception ligne) {
        lignes.add(ligne);
        ligne.setBonReception(this);
    }
}
