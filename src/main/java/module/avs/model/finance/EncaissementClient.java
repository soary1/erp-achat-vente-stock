package module.avs.model.finance;

import jakarta.persistence.*;
import lombok.*;
import module.avs.model.referentiel.ModePaiement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "encaissement_client")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EncaissementClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private FactureClient facture;
    
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal montant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mode_paiement_code", nullable = false)
    private ModePaiement modePaiement;
    
    @Column(name = "date_encaissement", nullable = false)
    private LocalDate dateEncaissement;
    
    @Column(length = 100)
    private String reference;
}
