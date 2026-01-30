package module.avs.repository.stock;

import module.avs.model.stock.BonReception;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BonReceptionRepository extends JpaRepository<BonReception, UUID> {
    Optional<BonReception> findByNumero(String numero);
    List<BonReception> findByCommandeAchatId(UUID commandeAchatId);
    List<BonReception> findByStatutCode(String statutCode);
    Page<BonReception> findAllByOrderByDateReceptionDesc(Pageable pageable);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(b.numero, 8) AS int)), 0) FROM BonReception b WHERE b.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
}
