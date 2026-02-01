package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.Utilisateur;
import module.avs.model.vente.CommandeClient;
import module.avs.model.vente.DevisClient;
import module.avs.model.vente.LigneCommandeClient;
import module.avs.model.vente.LigneDevisClient;
import module.avs.repository.vente.CommandeClientRepository;
import module.avs.repository.vente.DevisClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RemiseValidationService {
    
    private final DevisClientRepository devisClientRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final UtilisateurService utilisateurService;
    private final AuditService auditService;
    
    // Plafonds de remise par défaut par rôle
    private static final BigDecimal PLAFOND_COMMERCIAL = new BigDecimal("5.00"); // 5%
    private static final BigDecimal PLAFOND_MANAGER = new BigDecimal("15.00");   // 15%
    private static final BigDecimal PLAFOND_DIRECTEUR = new BigDecimal("30.00"); // 30%
    
    /**
     * Calcule le taux de remise total d'un devis (lignes + globale)
     */
    public BigDecimal calculerTauxRemiseTotal(DevisClient devis) {
        BigDecimal montantBrutTotal = devis.getLignes().stream()
            .map(l -> l.getPriceUnit().multiply(l.getQty()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (montantBrutTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal montantNetAvantRemiseGlobale = devis.getLignes().stream()
            .map(LigneDevisClient::getMontantHT)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal remiseLignes = montantBrutTotal.subtract(montantNetAvantRemiseGlobale);
        
        BigDecimal remiseGlobale = BigDecimal.ZERO;
        if (devis.getRemiseGlobalePct() != null && devis.getRemiseGlobalePct().compareTo(BigDecimal.ZERO) > 0) {
            remiseGlobale = montantNetAvantRemiseGlobale.multiply(devis.getRemiseGlobalePct())
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }
        
        BigDecimal remiseTotale = remiseLignes.add(remiseGlobale);
        
        return remiseTotale.multiply(new BigDecimal("100"))
            .divide(montantBrutTotal, 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calcule le taux de remise total d'une commande
     */
    public BigDecimal calculerTauxRemiseTotal(CommandeClient commande) {
        BigDecimal montantBrutTotal = commande.getLignes().stream()
            .map(l -> l.getPriceUnit().multiply(l.getQtyOrdered()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (montantBrutTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal montantNetAvantRemiseGlobale = commande.getLignes().stream()
            .map(LigneCommandeClient::getMontantHT)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal remiseLignes = montantBrutTotal.subtract(montantNetAvantRemiseGlobale);
        
        BigDecimal remiseGlobale = BigDecimal.ZERO;
        if (commande.getRemiseGlobalePct() != null && commande.getRemiseGlobalePct().compareTo(BigDecimal.ZERO) > 0) {
            remiseGlobale = montantNetAvantRemiseGlobale.multiply(commande.getRemiseGlobalePct())
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }
        
        BigDecimal remiseTotale = remiseLignes.add(remiseGlobale);
        
        return remiseTotale.multiply(new BigDecimal("100"))
            .divide(montantBrutTotal, 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Obtient le plafond de remise autorisé pour un utilisateur
     */
    public BigDecimal getPlafondRemiseUtilisateur(Utilisateur user) {
        // Récupérer le rôle le plus élevé de l'utilisateur
        if (utilisateurService.hasRole(user, "ADMIN") || utilisateurService.hasRole(user, "DIRECTEUR")) {
            return PLAFOND_DIRECTEUR;
        } else if (utilisateurService.hasRole(user, "MANAGER") || utilisateurService.hasRole(user, "RESPONSABLE_VENTES")) {
            return PLAFOND_MANAGER;
        } else {
            return PLAFOND_COMMERCIAL;
        }
    }
    
    /**
     * Vérifie si un devis nécessite une validation hiérarchique pour la remise
     */
    public ValidationRemiseResult verifierRemiseDevis(DevisClient devis, Utilisateur createur) {
        BigDecimal tauxRemise = calculerTauxRemiseTotal(devis);
        BigDecimal plafond = getPlafondRemiseUtilisateur(createur);
        
        if (tauxRemise.compareTo(plafond) > 0) {
            return new ValidationRemiseResult(
                false,
                "Remise de " + tauxRemise + "% dépasse le plafond autorisé (" + plafond + "%)",
                true,
                tauxRemise,
                plafond
            );
        }
        
        return new ValidationRemiseResult(
            true,
            "Remise autorisée",
            false,
            tauxRemise,
            plafond
        );
    }
    
    /**
     * Soumet un devis pour validation de remise
     */
    public DevisClient soumettreDevisPourValidation(UUID devisId, Utilisateur createur) {
        DevisClient devis = devisClientRepository.findById(devisId)
            .orElseThrow(() -> new RuntimeException("Devis non trouvé"));
        
        ValidationRemiseResult result = verifierRemiseDevis(devis, createur);
        
        if (result.requiresValidation()) {
            devis.setStatutCode("EN_ATTENTE_VALIDATION");
            DevisClient saved = devisClientRepository.save(devis);
            
            auditService.logWorkflow("DEVIS_CLIENT", devisId, "BROUILLON", "EN_ATTENTE_VALIDATION", 
                createur, "SOUMISSION_VALIDATION_REMISE", 
                "Remise: " + result.tauxRemise() + "% > Plafond: " + result.plafond() + "%");
            
            return saved;
        } else {
            // Validation automatique
            return validerDevisRemise(devisId, createur, "Validation automatique - remise dans les limites");
        }
    }
    
    /**
     * Valide la remise d'un devis (par un responsable)
     */
    public DevisClient validerDevisRemise(UUID devisId, Utilisateur validateur, String commentaire) {
        DevisClient devis = devisClientRepository.findById(devisId)
            .orElseThrow(() -> new RuntimeException("Devis non trouvé"));
        
        BigDecimal tauxRemise = calculerTauxRemiseTotal(devis);
        BigDecimal plafondValidateur = getPlafondRemiseUtilisateur(validateur);
        
        if (tauxRemise.compareTo(plafondValidateur) > 0) {
            throw new RuntimeException("Remise de " + tauxRemise + "% dépasse votre plafond autorisé (" + plafondValidateur + "%)");
        }
        
        devis.setStatutCode("VALIDE");
        devis.setValidateur(validateur);
        devis.setDateValidation(OffsetDateTime.now());
        devis.setNotes(commentaire);
        
        DevisClient saved = devisClientRepository.save(devis);
        
        auditService.logWorkflow("DEVIS_CLIENT", devisId, "EN_ATTENTE_VALIDATION", "VALIDE", 
            validateur, "VALIDATION_REMISE", commentaire);
        
        return saved;
    }
    
    /**
     * Refuse un devis (remise trop élevée)
     */
    public DevisClient refuserDevisRemise(UUID devisId, Utilisateur validateur, String motif) {
        DevisClient devis = devisClientRepository.findById(devisId)
            .orElseThrow(() -> new RuntimeException("Devis non trouvé"));
        
        devis.setStatutCode("REFUSE");
        devis.setValidateur(validateur);
        devis.setDateValidation(OffsetDateTime.now());
        devis.setMotifRefus(motif);
        
        DevisClient saved = devisClientRepository.save(devis);
        
        auditService.logWorkflow("DEVIS_CLIENT", devisId, devis.getStatutCode(), "REFUSE", 
            validateur, "REFUS_REMISE", motif);
        
        return saved;
    }
    
    /**
     * Vérifie si une commande nécessite une validation hiérarchique pour la remise
     */
    public ValidationRemiseResult verifierRemiseCommande(CommandeClient commande, Utilisateur createur) {
        BigDecimal tauxRemise = calculerTauxRemiseTotal(commande);
        BigDecimal plafond = getPlafondRemiseUtilisateur(createur);
        
        if (tauxRemise.compareTo(plafond) > 0) {
            return new ValidationRemiseResult(
                false,
                "Remise de " + tauxRemise + "% dépasse le plafond autorisé (" + plafond + "%)",
                true,
                tauxRemise,
                plafond
            );
        }
        
        return new ValidationRemiseResult(
            true,
            "Remise autorisée",
            false,
            tauxRemise,
            plafond
        );
    }
    
    /**
     * Soumet une commande pour validation de remise
     */
    public CommandeClient soumettreCommandePourValidation(UUID commandeId, Utilisateur createur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        ValidationRemiseResult result = verifierRemiseCommande(commande, createur);
        
        if (result.requiresValidation()) {
            commande.setStatutCode("EN_ATTENTE_VALIDATION");
            CommandeClient saved = commandeClientRepository.save(commande);
            
            auditService.logWorkflow("COMMANDE_CLIENT", commandeId, "BROUILLON", "EN_ATTENTE_VALIDATION", 
                createur, "SOUMISSION_VALIDATION_REMISE", 
                "Remise: " + result.tauxRemise() + "% > Plafond: " + result.plafond() + "%");
            
            return saved;
        } else {
            // Passer directement en CONFIRMEE si la remise est OK
            commande.setStatutCode("CONFIRMEE");
            commande.setValidateur(createur);
            commande.setDateValidation(OffsetDateTime.now());
            return commandeClientRepository.save(commande);
        }
    }
    
    /**
     * Valide la remise d'une commande (par un responsable)
     */
    public CommandeClient validerCommandeRemise(UUID commandeId, Utilisateur validateur, String commentaire) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        BigDecimal tauxRemise = calculerTauxRemiseTotal(commande);
        BigDecimal plafondValidateur = getPlafondRemiseUtilisateur(validateur);
        
        if (tauxRemise.compareTo(plafondValidateur) > 0) {
            throw new RuntimeException("Remise de " + tauxRemise + "% dépasse votre plafond autorisé (" + plafondValidateur + "%)");
        }
        
        commande.setStatutCode("CONFIRMEE");
        commande.setValidateur(validateur);
        commande.setDateValidation(OffsetDateTime.now());
        commande.setNotes(commentaire);
        
        CommandeClient saved = commandeClientRepository.save(commande);
        
        auditService.logWorkflow("COMMANDE_CLIENT", commandeId, "EN_ATTENTE_VALIDATION", "CONFIRMEE", 
            validateur, "VALIDATION_REMISE", commentaire);
        
        return saved;
    }
    
    /**
     * Résultat de la validation de remise
     */
    public record ValidationRemiseResult(
        boolean autorise,
        String message,
        boolean requiresValidation,
        BigDecimal tauxRemise,
        BigDecimal plafond
    ) {}
}
