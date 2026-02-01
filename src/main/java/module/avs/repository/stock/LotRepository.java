package module.avs.repository.stock;

import module.avs.model.stock.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LotRepository extends JpaRepository<Lot, UUID> {
    Optional<Lot> findByNumeroLot(String numeroLot);
    Optional<Lot> findByArticleIdAndNumeroLot(UUID articleId, String numeroLot);
    List<Lot> findByArticleId(UUID articleId);
    
    @Query("SELECT l FROM Lot l WHERE l.datePeremption < :date AND l.statutQualiteCode = 'CONFORME'")
    List<Lot> findExpiredLots(LocalDate date);
    
    @Query("SELECT l FROM Lot l WHERE l.datePeremption BETWEEN :now AND :soon")
    List<Lot> findLotsExpiringSoon(LocalDate now, LocalDate soon);
}
