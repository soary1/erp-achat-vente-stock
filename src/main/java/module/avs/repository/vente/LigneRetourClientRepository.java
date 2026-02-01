package module.avs.repository.vente;

import module.avs.model.vente.LigneRetourClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneRetourClientRepository extends JpaRepository<LigneRetourClient, UUID> {
    List<LigneRetourClient> findByRetourId(UUID retourId);
    List<LigneRetourClient> findByArticleId(UUID articleId);
    List<LigneRetourClient> findByLotId(UUID lotId);
}
