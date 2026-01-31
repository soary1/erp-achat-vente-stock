package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.achat.*;
import module.avs.model.security.Utilisateur;
import module.avs.repository.achat.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AchatService {
    
    private final DemandeAchatRepository demandeAchatRepository;
    private final LigneDemandeAchatRepository ligneDemandeAchatRepository;
    private final CommandeAchatRepository commandeAchatRepository;
    private final LigneCommandeAchatRepository ligneCommandeAchatRepository;
    private final AuditService auditService;
    private final UtilisateurService utilisateurService;
    
    // ============ DEMANDES D'ACHAT ============
    
    public List<DemandeAchat> findAllDemandesAchat() {
        return demandeAchatRepository.findAll();
    }
    
    public Page<DemandeAchat> findAllDemandesAchat(Pageable pageable) {
        return demandeAchatRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Optional<DemandeAchat> findDemandeAchatById(UUID id) {
        return demandeAchatRepository.findByIdWithDetails(id);
    }
    
    public List<DemandeAchat> findDemandesAchatByStatut(String statut) {
        return demandeAchatRepository.findByStatutCodeWithDetails(statut);
    }
    
    public String generateDemandeAchatNumero() {
        String prefix = "DA-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = demandeAchatRepository.findMaxNumero(prefix + "%");
        return prefix + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);
    }
    
    public DemandeAchat createDemandeAchat(DemandeAchat demande, Utilisateur createur) {
        demande.setNumero(generateDemandeAchatNumero());
        demande.setStatutCode("BROUILLON");
        demande.setDemandeur(createur);
        
        // Set the demandeAchat reference on each ligne before saving
        if (demande.getLignes() != null) {
            for (LigneDemandeAchat ligne : demande.getLignes()) {
                ligne.setDemandeAchat(demande);
            }
        }
        
        DemandeAchat saved = demandeAchatRepository.save(demande);
        
        auditService.logAction("DEMANDE_ACHAT", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public DemandeAchat saveDemandeAchat(DemandeAchat demande) {
        return demandeAchatRepository.save(demande);
    }
    
    public DemandeAchat soumettreDemande(UUID demandeId, Utilisateur acteur) {
        DemandeAchat demande = demandeAchatRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        String ancienStatut = demande.getStatutCode();
        demande.setStatutCode("SOUMISE");
        DemandeAchat saved = demandeAchatRepository.save(demande);
        
        auditService.logWorkflow("DEMANDE_ACHAT", demandeId, ancienStatut, "SOUMISE", acteur, "SOUMISSION", null);
        return saved;
    }
    
    public DemandeAchat approuverDemande(UUID demandeId, Utilisateur approbateur, String commentaire) {
        DemandeAchat demande = demandeAchatRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        // Vérification séparation des tâches
        if (!utilisateurService.canApproveDocument(approbateur.getId(), demande.getDemandeur().getId())) {
            throw new RuntimeException("Vous ne pouvez pas approuver votre propre demande");
        }
        
        String ancienStatut = demande.getStatutCode();
        demande.setStatutCode("APPROUVEE");
        DemandeAchat saved = demandeAchatRepository.save(demande);
        
        auditService.logWorkflow("DEMANDE_ACHAT", demandeId, ancienStatut, "APPROUVEE", approbateur, "APPROBATION", commentaire);
        return saved;
    }
    
    public DemandeAchat rejeterDemande(UUID demandeId, Utilisateur acteur, String motif) {
        DemandeAchat demande = demandeAchatRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        String ancienStatut = demande.getStatutCode();
        demande.setStatutCode("REJETEE");
        DemandeAchat saved = demandeAchatRepository.save(demande);
        
        auditService.logWorkflow("DEMANDE_ACHAT", demandeId, ancienStatut, "REJETEE", acteur, "REJET", motif);
        return saved;
    }
    
    // ============ COMMANDES D'ACHAT ============
    
    public List<CommandeAchat> findAllCommandesAchat() {
        return commandeAchatRepository.findAll();
    }
    
    public Page<CommandeAchat> findAllCommandesAchat(Pageable pageable) {
        return commandeAchatRepository.findAllWithDetails(pageable);
    }
    
    public Optional<CommandeAchat> findCommandeAchatById(UUID id) {
        return commandeAchatRepository.findByIdWithDetails(id);
    }
    
    public List<CommandeAchat> findCommandesAchatByStatut(String statut) {
        return commandeAchatRepository.findByStatutCodeWithDetails(statut);
    }
    
    public String generateCommandeAchatNumero() {
        String prefix = "BC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = commandeAchatRepository.findMaxNumero(prefix + "%");
        return prefix + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);
    }
    
    public CommandeAchat createCommandeAchat(CommandeAchat commande, Utilisateur createur) {
        commande.setNumero(generateCommandeAchatNumero());
        commande.setStatutCode("BROUILLON");
        commande.setAcheteur(createur);
        // Assurer que chaque ligne a la référence à la commande
        for (LigneCommandeAchat ligne : commande.getLignes()) {
            ligne.setCommande(commande);
        }
        commande.recalculerTotaux();
        CommandeAchat saved = commandeAchatRepository.save(commande);
        
        auditService.logAction("COMMANDE_ACHAT", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public CommandeAchat createCommandeFromDemande(UUID demandeId, CommandeAchat commande, Utilisateur createur) {
        DemandeAchat demande = demandeAchatRepository.findById(demandeId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        if (!"APPROUVEE".equals(demande.getStatutCode())) {
            throw new RuntimeException("La demande doit être approuvée");
        }
        
        commande.setDemandeAchat(demande);
        return createCommandeAchat(commande, createur);
    }
    
    public CommandeAchat saveCommandeAchat(CommandeAchat commande) {
        commande.recalculerTotaux();
        return commandeAchatRepository.save(commande);
    }
    
    public CommandeAchat validerCommande(UUID commandeId, Utilisateur validateur) {
        CommandeAchat commande = commandeAchatRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        // Vérification séparation des tâches
        if (!utilisateurService.canApproveDocument(validateur.getId(), commande.getAcheteur().getId())) {
            throw new RuntimeException("Vous ne pouvez pas valider votre propre commande");
        }
        
        // Vérification du montant
        if (!utilisateurService.canApproveAmount(validateur.getId(), commande.getTotalTTC())) {
            throw new RuntimeException("Montant supérieur à votre plafond d'approbation");
        }
        
        String ancienStatut = commande.getStatutCode();
        commande.setStatutCode("VALIDEE");
        CommandeAchat saved = commandeAchatRepository.save(commande);
        
        auditService.logWorkflow("COMMANDE_ACHAT", commandeId, ancienStatut, "VALIDEE", validateur, "VALIDATION", null);
        return saved;
    }
    
    public CommandeAchat envoyerCommande(UUID commandeId, Utilisateur acteur) {
        CommandeAchat commande = commandeAchatRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        String ancienStatut = commande.getStatutCode();
        commande.setStatutCode("ENVOYEE");
        CommandeAchat saved = commandeAchatRepository.save(commande);
        
        auditService.logWorkflow("COMMANDE_ACHAT", commandeId, ancienStatut, "ENVOYEE", acteur, "ENVOI", null);
        return saved;
    }
    
    public void updateStatutCommandeApresReception(UUID commandeId) {
        CommandeAchat commande = commandeAchatRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        boolean toutRecu = commande.getLignes().stream()
            .allMatch(l -> l.getQtyRestante().compareTo(java.math.BigDecimal.ZERO) <= 0);
        
        if (toutRecu) {
            commande.setStatutCode("CLOTUREE");
        } else {
            commande.setStatutCode("PARTIEL");
        }
        commandeAchatRepository.save(commande);
    }
}
