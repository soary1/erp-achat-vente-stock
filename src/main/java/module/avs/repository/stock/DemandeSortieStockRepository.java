package module.avs.repository.stock;

import module.avs.model.stock.DemandeSortieStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemandeSortieStockRepository extends JpaRepository<DemandeSortieStock, UUID> {
    Optional<DemandeSortieStock> findByNumero(String numero);
    List<DemandeSortieStock> findByStatutCode(String statutCode);
    List<DemandeSortieStock> findByType(String type);
    Page<DemandeSortieStock> findAllByOrderByDateDemandeDesc(Pageable pageable);
    Page<DemandeSortieStock> findByTypeOrderByDateDemandeDesc(String type, Pageable pageable);
    List<DemandeSortieStock> findByDepotId(UUID depotId);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(d.numero, 9) AS int)), 0) FROM DemandeSortieStock d WHERE d.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
    
    @Query("SELECT COUNT(d) FROM DemandeSortieStock d WHERE d.statutCode = :statut")
    Long countByStatut(String statut);
    
    @Query("SELECT COUNT(d) FROM DemandeSortieStock d WHERE d.type = :type AND d.statutCode = :statut")
    Long countByTypeAndStatut(String type, String statut);
}
