package module.avs.repository.stock;

import module.avs.model.stock.Inventaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventaireRepository extends JpaRepository<Inventaire, UUID> {
    Optional<Inventaire> findByNumero(String numero);
    List<Inventaire> findByStatutCode(String statutCode);
    List<Inventaire> findByDepotId(UUID depotId);
    Page<Inventaire> findAllByOrderByDateDebutDesc(Pageable pageable);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.numero, LENGTH(i.numero) - 2) AS int)), 0) FROM Inventaire i WHERE i.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
}
