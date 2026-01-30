package module.avs.repository.tiers;

import module.avs.model.tiers.Fournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, UUID> {
    Optional<Fournisseur> findByCode(String code);
    List<Fournisseur> findByIsActiveTrue();
    
    @Query("SELECT f FROM Fournisseur f WHERE f.isActive = true AND (LOWER(f.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Fournisseur> searchFournisseurs(String search, Pageable pageable);
}
