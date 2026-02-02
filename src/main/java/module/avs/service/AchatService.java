package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.achat.*;
import module.avs.model.security.Utilisateur;
import module.avs.repository.achat.*;
import module.avs.repository.stock.LigneBonReceptionRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map;

@Service
@Transactional
public class AchatService {
    
    private final DemandeAchatRepository demandeAchatRepository;
    private final LigneDemandeAchatRepository ligneDemandeAchatRepository;
    private final CommandeAchatRepository commandeAchatRepository;
    private final LigneCommandeAchatRepository ligneCommandeAchatRepository;
    private final LigneBonReceptionRepository ligneBonReceptionRepository;
    private final AuditService auditService;
    private final UtilisateurService utilisateurService;
    private final FinanceService financeService;
    
    public AchatService(
            DemandeAchatRepository demandeAchatRepository,
            LigneDemandeAchatRepository ligneDemandeAchatRepository,
            CommandeAchatRepository commandeAchatRepository,
            LigneCommandeAchatRepository ligneCommandeAchatRepository,
            LigneBonReceptionRepository ligneBonReceptionRepository,
            AuditService auditService,
            UtilisateurService utilisateurService,
            @Lazy FinanceService financeService) {
        this.demandeAchatRepository = demandeAchatRepository;
        this.ligneDemandeAchatRepository = ligneDemandeAchatRepository;
        this.commandeAchatRepository = commandeAchatRepository;
        this.ligneCommandeAchatRepository = ligneCommandeAchatRepository;
        this.ligneBonReceptionRepository = ligneBonReceptionRepository;
        this.auditService = auditService;
        this.utilisateurService = utilisateurService;
        this.financeService = financeService;
    }
    
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
    
    public synchronized String generateDemandeAchatNumero() {
        String prefix = "DA-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = demandeAchatRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (demandeAchatRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
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
    
    public synchronized String generateCommandeAchatNumero() {
        String prefix = "BC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = commandeAchatRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        // Vérification pour éviter les doublons
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (commandeAchatRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
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
        // Si pas de numéro (création), en générer un
        if (commande.getNumero() == null || commande.getNumero().isEmpty()) {
            commande.setNumero(generateCommandeAchatNumero());
        }
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
        
        // CRÉATION AUTOMATIQUE DE LA FACTURE FOURNISSEUR
        try {
            financeService.createFactureFromCommandeAchat(saved, acteur);
        } catch (Exception e) {
            // Log error but don't fail the transaction
            auditService.logAction("FACTURE_FOURNISSEUR", null, "CREATION_AUTO_FAILED", acteur, 
                Map.of("commandeAchatId", commandeId.toString(), "error", e.getMessage()));
        }
        
        auditService.logWorkflow("COMMANDE_ACHAT", commandeId, ancienStatut, "ENVOYEE", acteur, "ENVOI", null);
        return saved;
    }
    
    public void updateStatutCommandeApresReception(UUID commandeId) {
        CommandeAchat commande = commandeAchatRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        // Vérifier si toutes les lignes de commande ont été entièrement reçues
        boolean toutRecu = commande.getLignes().stream()
            .allMatch(ligne -> {
                BigDecimal qtyCommandee = ligne.getQtyOrdered();
                BigDecimal qtyRecue = ligneBonReceptionRepository.sumQtyReceivedForCommandeAndArticle(
                    commandeId, 
                    ligne.getArticle().getId()
                );
                return qtyRecue.compareTo(qtyCommandee) >= 0;
            });
        
        if (toutRecu) {
            commande.setStatutCode("CLOTUREE");
        } else {
            commande.setStatutCode("PARTIEL");
        }
        commandeAchatRepository.save(commande);
    }
}
