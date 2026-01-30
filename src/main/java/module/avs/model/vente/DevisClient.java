package module.avs.model.vente;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.organisation.Site;
import module.avs.model.tiers.Client;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "devis_client")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DevisClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 50, unique = true)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
    @Column(name = "statut_code", length = 50)
    @Builder.Default
    private String statutCode = "BROUILLON";
    
    @Column(name = "total_ht", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalHT = BigDecimal.ZERO;
    
    @Column(name = "total_ttc", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalTTC = BigDecimal.ZERO;
    
    @Column(name = "date_validite")
    private java.time.LocalDate dateValidite;
    
    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneDevisClient> lignes = new ArrayList<>();
    
    public void addLigne(LigneDevisClient ligne) {
        lignes.add(ligne);
        ligne.setDevis(this);
    }
    
    public void recalculerTotaux() {
        this.totalHT = lignes.stream()
            .map(l -> l.getPriceUnit().multiply(l.getQty()))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        this.totalTTC = this.totalHT;
    }
}
