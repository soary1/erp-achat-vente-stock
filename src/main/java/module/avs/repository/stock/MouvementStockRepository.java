package module.avs.repository.stock;

import module.avs.model.stock.MouvementStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, UUID> {
    
    // ============ REQUÊTES DE BASE ============
    
    Optional<MouvementStock> findByNumero(String numero);
    List<MouvementStock> findByArticleIdOrderByCreatedAtDesc(UUID articleId);
    List<MouvementStock> findByReferenceDoc(String referenceDoc);
    Page<MouvementStock> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT m FROM MouvementStock m WHERE m.createdAt BETWEEN :start AND :end ORDER BY m.createdAt DESC")
    List<MouvementStock> findByPeriod(OffsetDateTime start, OffsetDateTime end);
    
    @Query("SELECT m FROM MouvementStock m WHERE m.depotSource.id = :depotId OR m.depotDest.id = :depotId ORDER BY m.createdAt DESC")
    List<MouvementStock> findByDepot(UUID depotId);
    
    // ============ NUMÉROTATION ============
    
    @Query("SELECT MAX(CAST(SUBSTRING(m.numero, LENGTH(:prefix) + 1) AS int)) FROM MouvementStock m WHERE m.numero LIKE CONCAT(:prefix, '%')")
    Integer findMaxNumero(String prefix);
    
    // ============ TRAÇABILITÉ LOT ============
    
    List<MouvementStock> findByLotIdOrderByCreatedAtAsc(UUID lotId);
    
    // ============ FILTRES AVANCÉS ============
    
    // Filtre par type
    Page<MouvementStock> findByTypeMouvementCodeOrderByCreatedAtDesc(String code, Pageable pageable);
    
    // Filtre par article
    Page<MouvementStock> findByArticleIdOrderByCreatedAtDesc(UUID articleId, Pageable pageable);
    
    // Filtre par dépôt (source ou dest)
    @Query("SELECT m FROM MouvementStock m WHERE m.depotSource.id = :depotId OR m.depotDest.id = :depotId ORDER BY m.createdAt DESC")
    Page<MouvementStock> findByDepotIdPaged(UUID depotId, Pageable pageable);
    
    // Filtre par période
    @Query("SELECT m FROM MouvementStock m WHERE m.createdAt >= :dateDebut AND m.createdAt <= :dateFin ORDER BY m.createdAt DESC")
    Page<MouvementStock> findByPeriodPaged(OffsetDateTime dateDebut, OffsetDateTime dateFin, Pageable pageable);
    
    // Filtre combiné type + article
    Page<MouvementStock> findByTypeMouvementCodeAndArticleIdOrderByCreatedAtDesc(String code, UUID articleId, Pageable pageable);
    
    // ============ BLOCAGE DELETE (Sécurité) ============
    // Les mouvements sont IMMUTABLES - ces méthodes lèvent une exception
    
    @Override
    default void delete(MouvementStock entity) {
        throw new UnsupportedOperationException("Les mouvements de stock ne peuvent pas être supprimés");
    }
    
    @Override
    default void deleteById(UUID id) {
        throw new UnsupportedOperationException("Les mouvements de stock ne peuvent pas être supprimés");
    }
    
    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Les mouvements de stock ne peuvent pas être supprimés");
    }
    
    @Override
    default void deleteAll(Iterable<? extends MouvementStock> entities) {
        throw new UnsupportedOperationException("Les mouvements de stock ne peuvent pas être supprimés");
    }
    
    // Méthodes pour récupération du coût unitaire
    List<MouvementStock> findByArticleIdAndDepotDestIdAndUnitCostIsNotNullOrderByCreatedAtDesc(UUID articleId, UUID depotId);
    
    List<MouvementStock> findByArticleIdAndLotIdAndUnitCostIsNotNullOrderByCreatedAtDesc(UUID articleId, UUID lotId);
}
