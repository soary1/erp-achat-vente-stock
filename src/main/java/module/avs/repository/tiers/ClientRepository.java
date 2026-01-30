package module.avs.repository.tiers;

import module.avs.model.tiers.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByCode(String code);
    List<Client> findByIsActiveTrue();
    
    @Query("SELECT c FROM Client c WHERE c.isActive = true AND (LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Client> searchClients(String search, Pageable pageable);
}
