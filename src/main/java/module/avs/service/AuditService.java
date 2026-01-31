package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.*;
import module.avs.repository.security.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {
    
    private final JournalAuditRepository journalAuditRepository;
    private final HistoriqueWorkflowRepository historiqueWorkflowRepository;
    private final ObjectMapper objectMapper;
    
    public void logAction(String entityName, UUID entityId, String action, Utilisateur user, Object changes) {
        JsonNode changesJson = changes != null ? objectMapper.convertValue(changes, JsonNode.class) : objectMapper.createObjectNode();
        JournalAudit audit = JournalAudit.builder()
            .entityName(entityName)
            .entityId(entityId)
            .action(action)
            .utilisateur(user)
            .changes(changesJson)
            .createdAt(OffsetDateTime.now())
            .build();
        journalAuditRepository.save(audit);
    }
    
    public void logWorkflow(String documentType, UUID documentId, String etapePrecedente, 
                           String etapeNouvelle, Utilisateur acteur, String action, String commentaire) {
        HistoriqueWorkflow workflow = HistoriqueWorkflow.builder()
            .documentType(documentType)
            .documentId(documentId)
            .etapePrecedente(etapePrecedente)
            .etapeNouvelle(etapeNouvelle)
            .acteur(acteur)
            .action(action)
            .commentaire(commentaire)
            .createdAt(OffsetDateTime.now())
            .build();
        historiqueWorkflowRepository.save(workflow);
    }
    
    public List<HistoriqueWorkflow> getWorkflowHistory(String documentType, UUID documentId) {
        return historiqueWorkflowRepository.findByDocumentTypeAndDocumentIdOrderByCreatedAtDesc(documentType, documentId);
    }
    
    public List<JournalAudit> getAuditHistory(String entityName, UUID entityId) {
        return journalAuditRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId);
    }
}
