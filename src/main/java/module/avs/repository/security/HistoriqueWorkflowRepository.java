package module.avs.repository.security;

import module.avs.model.security.HistoriqueWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface HistoriqueWorkflowRepository extends JpaRepository<HistoriqueWorkflow, UUID> {
    List<HistoriqueWorkflow> findByDocumentTypeAndDocumentIdOrderByCreatedAtDesc(String documentType, UUID documentId);
}
