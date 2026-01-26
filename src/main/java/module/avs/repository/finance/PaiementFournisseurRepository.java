package module.avs.repository.finance;

import module.avs.model.finance.PaiementFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaiementFournisseurRepository extends JpaRepository<PaiementFournisseur, UUID> {
    List<PaiementFournisseur> findByFactureId(UUID factureId);
    
    @Query("SELECT SUM(p.montant) FROM PaiementFournisseur p WHERE p.datePaiement BETWEEN :start AND :end")
    BigDecimal sumPaiementsByPeriod(LocalDate start, LocalDate end);
}
