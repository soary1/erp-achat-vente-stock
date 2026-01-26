package module.avs.repository.stock;

import module.avs.model.stock.LigneBonReception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneBonReceptionRepository extends JpaRepository<LigneBonReception, UUID> {
    List<LigneBonReception> findByBonReceptionId(UUID bonReceptionId);
}
