package module.avs.repository.stock;

import module.avs.model.stock.TransfertStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransfertStockRepository extends JpaRepository<TransfertStock, UUID> {
    Optional<TransfertStock> findByNumero(String numero);
    List<TransfertStock> findByStatutCode(String statutCode);
    Page<TransfertStock> findAllByOrderByDateDemandeDesc(Pageable pageable);
    List<TransfertStock> findByDepotSourceId(UUID depotSourceId);
    List<TransfertStock> findByDepotDestId(UUID depotDestId);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(t.numero, 9) AS int)), 0) FROM TransfertStock t WHERE t.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    @Query("SELECT COUNT(t) FROM TransfertStock t WHERE t.statutCode = :statut")
    Long countByStatut(String statut);
    
    @Query("SELECT t FROM TransfertStock t WHERE t.depotSource.id = :depotId OR t.depotDest.id = :depotId ORDER BY t.dateDemande DESC")
    List<TransfertStock> findByDepotImplique(UUID depotId);
}
