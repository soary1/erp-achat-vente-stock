package module.avs.repository.finance;

import module.avs.model.finance.RapprochementAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RapprochementAchatRepository extends JpaRepository<RapprochementAchat, UUID> {
    List<RapprochementAchat> findByFactureId(UUID factureId);
    List<RapprochementAchat> findByReceptionId(UUID receptionId);
}
