package module.avs.repository.vente;

import module.avs.model.vente.DevisClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DevisClientRepository extends JpaRepository<DevisClient, UUID> {
    Optional<DevisClient> findByNumero(String numero);
    List<DevisClient> findByClientId(UUID clientId);
    List<DevisClient> findByStatutCode(String statutCode);
    Page<DevisClient> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(d.numero, 9) AS int)), 0) FROM DevisClient d WHERE d.numero LIKE :prefix")
    Integer findMaxNumero(String prefix);
}
