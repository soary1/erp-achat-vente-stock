package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.achat.CommandeAchat;
import module.avs.model.finance.*;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.BonReception;
import module.avs.model.vente.BonLivraison;
import module.avs.model.vente.CommandeClient;
import module.avs.model.vente.LigneBonLivraison;
import module.avs.repository.finance.*;
import module.avs.repository.stock.BonReceptionRepository;
import module.avs.repository.vente.BonLivraisonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {
    
    private final FactureFournisseurRepository factureFournisseurRepository;
    private final RapprochementAchatRepository rapprochementAchatRepository;
    private final PaiementFournisseurRepository paiementFournisseurRepository;
    private final FactureClientRepository factureClientRepository;
    private final EncaissementClientRepository encaissementClientRepository;
    private final BonReceptionRepository bonReceptionRepository;
    private final BonLivraisonRepository bonLivraisonRepository;
    private final AuditService auditService;
    
    // ============ FACTURES FOURNISSEUR ============
    
    public List<FactureFournisseur> findAllFacturesFournisseur() {
        return factureFournisseurRepository.findAll();
    }
    
    public Page<FactureFournisseur> findAllFacturesFournisseur(Pageable pageable) {
        return factureFournisseurRepository.findAllByOrderByDateFactureDesc(pageable);
    }
    
    public Optional<FactureFournisseur> findFactureFournisseurById(UUID id) {
        return factureFournisseurRepository.findById(id);
    }
    
    public FactureFournisseur createFactureFournisseur(FactureFournisseur facture, Utilisateur createur) {
        facture.setStatutCode("BROUILLON");
        FactureFournisseur saved = factureFournisseurRepository.save(facture);
        
        auditService.logAction("FACTURE_FOURNISSEUR", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public FactureFournisseur saveFactureFournisseur(FactureFournisseur facture) {
        return factureFournisseurRepository.save(facture);
    }
    
    // ============ RAPPROCHEMENT 3-WAY MATCH ============
    
    /**
     * Effectue le rapprochement 3-way match entre:
     * - Commande d'achat (BC)
     * - Bon de réception (BR)  
     * - Facture fournisseur
     */
    public RapprochementResult effectuerRapprochement(UUID factureId, UUID receptionId) {
        FactureFournisseur facture = factureFournisseurRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
        
        BonReception reception = bonReceptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("Réception non trouvée"));
        
        CommandeAchat commande = reception.getCommandeAchat();
        if (commande == null) {
            throw new RuntimeException("La réception n'est pas liée à une commande");
        }
        
        // Calculer les montants
        BigDecimal montantCommande = commande.getTotalTTC();
        BigDecimal montantReception = reception.getLignes().stream()
            .map(l -> l.getQtyReceived().multiply(l.getUnitCost() != null ? l.getUnitCost() : BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montantFacture = facture.getMontantTTC();
        
        // Vérifier les écarts
        BigDecimal ecartCmdFacture = montantCommande.subtract(montantFacture).abs();
        BigDecimal ecartRecFacture = montantReception.subtract(montantFacture).abs();
        BigDecimal toleranceMax = montantFacture.multiply(new BigDecimal("0.01")); // 1% de tolérance
        
        boolean isMatch = ecartCmdFacture.compareTo(toleranceMax) <= 0 
                       && ecartRecFacture.compareTo(toleranceMax) <= 0;
        
        // Créer le rapprochement
        RapprochementAchat rapprochement = RapprochementAchat.builder()
            .facture(facture)
            .reception(reception)
            .montantRapproche(montantFacture)
            .isMatch(isMatch)
            .commentaire(isMatch ? "Rapprochement OK" : "Écart détecté - Vérification requise")
            .build();
        
        rapprochementAchatRepository.save(rapprochement);
        
        // Mettre à jour le statut de la facture si match OK
        if (isMatch) {
            facture.setStatutCode("A_PAYER");
            factureFournisseurRepository.save(facture);
        }
        
        return new RapprochementResult(isMatch, montantCommande, montantReception, montantFacture, 
                                       ecartCmdFacture, ecartRecFacture);
    }
    
    public record RapprochementResult(
        boolean isMatch,
        BigDecimal montantCommande,
        BigDecimal montantReception,
        BigDecimal montantFacture,
        BigDecimal ecartCmdFacture,
        BigDecimal ecartRecFacture
    ) {}
    
    // ============ PAIEMENTS FOURNISSEUR ============
    
    public PaiementFournisseur enregistrerPaiement(PaiementFournisseur paiement, Utilisateur acteur) {
        FactureFournisseur facture = paiement.getFacture();
        
        PaiementFournisseur saved = paiementFournisseurRepository.save(paiement);
        
        // Mettre à jour le montant payé
        facture.setMontantPaye(facture.getMontantPaye().add(paiement.getMontant()));
        
        // Mettre à jour le statut
        if (facture.getMontantRestant().compareTo(BigDecimal.ZERO) <= 0) {
            facture.setStatutCode("PAYEE");
        } else {
            facture.setStatutCode("PAYEE_PARTIEL");
        }
        factureFournisseurRepository.save(facture);
        
        auditService.logAction("PAIEMENT_FOURNISSEUR", saved.getId(), "CREATION", acteur, null);
        return saved;
    }
    
    // ============ FACTURES CLIENT ============
    
    public List<FactureClient> findAllFacturesClient() {
        return factureClientRepository.findAll();
    }
    
    public Page<FactureClient> findAllFacturesClient(Pageable pageable) {
        return factureClientRepository.findAllByOrderByDateFactureDesc(pageable);
    }
    
    public Optional<FactureClient> findFactureClientById(UUID id) {
        return factureClientRepository.findById(id);
    }
    
    public synchronized String generateFactureClientNumero() {
        String prefix = "FAC-" + LocalDate.now().getYear() + "-";
        Integer maxNum = factureClientRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%05d", nextNum);
            if (factureClientRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    /**
     * Crée une facture depuis une commande client
     * @deprecated Utiliser createFactureFromBonLivraison pour respecter le workflow
     */
    @Deprecated
    public FactureClient createFactureClient(CommandeClient commande, Utilisateur createur) {
        FactureClient facture = FactureClient.builder()
            .numero(generateFactureClientNumero())
            .client(commande.getClient())
            .commande(commande)
            .montantHT(commande.getTotalHT())
            .montantTTC(commande.getTotalTTC())
            .statutCode("A_PAYER")
            .dateFacture(LocalDate.now())
            .dateEcheance(LocalDate.now().plusDays(30))
            .createur(createur)
            .build();
        
        FactureClient saved = factureClientRepository.save(facture);
        
        auditService.logAction("FACTURE_CLIENT", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    /**
     * Crée une facture depuis un bon de livraison validé
     * C'est la méthode recommandée selon le workflow
     */
    public FactureClient createFactureFromBonLivraison(UUID bonLivraisonId, Utilisateur createur) {
        BonLivraison bonLivraison = bonLivraisonRepository.findById(bonLivraisonId)
            .orElseThrow(() -> new RuntimeException("Bon de livraison non trouvé"));
        
        // Vérifier si une facture existe déjà pour ce BL
        Optional<FactureClient> existing = factureClientRepository.findByBonLivraisonId(bonLivraisonId);
        if (existing.isPresent()) {
            return existing.get(); // Retourner la facture existante
        }
        
        CommandeClient commande = bonLivraison.getCommande();
        
        // Calculer le montant basé sur les quantités livrées
        BigDecimal montantHT = BigDecimal.ZERO;
        for (LigneBonLivraison ligne : bonLivraison.getLignes()) {
            // Récupérer le prix depuis la ligne de commande
            BigDecimal prixUnitaire = commande.getLignes().stream()
                .filter(lc -> lc.getArticle().getId().equals(ligne.getArticle().getId()))
                .findFirst()
                .map(lc -> lc.getPriceUnit())
                .orElse(BigDecimal.ZERO);
            
            BigDecimal montantLigne = prixUnitaire.multiply(ligne.getQtyLivree());
            montantHT = montantHT.add(montantLigne);
        }
        
        // Appliquer la remise globale si présente
        if (commande.getRemiseGlobalePct() != null && commande.getRemiseGlobalePct().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remise = montantHT.multiply(commande.getRemiseGlobalePct()).divide(new BigDecimal("100"));
            montantHT = montantHT.subtract(remise);
        }
        
        // Appliquer TVA 20%
        BigDecimal tva = montantHT.multiply(new BigDecimal("0.20"));
        BigDecimal montantTTC = montantHT.add(tva);
        
        FactureClient facture = FactureClient.builder()
            .numero(generateFactureClientNumero())
            .client(commande.getClient())
            .commande(commande)
            .bonLivraison(bonLivraison)
            .montantHT(montantHT)
            .montantTTC(montantTTC)
            .statutCode("A_PAYER")
            .dateFacture(LocalDate.now())
            .dateEcheance(LocalDate.now().plusDays(30))
            .createur(createur)
            .build();
        
        FactureClient saved = factureClientRepository.save(facture);
        
        auditService.logAction("FACTURE_CLIENT", saved.getId(), "CREATION_AUTO_BL", createur, 
            Map.of("bonLivraisonId", bonLivraisonId.toString(), "bonLivraisonNumero", bonLivraison.getNumero()));
        
        return saved;
    }
    
    /**
     * Crée une facture fournisseur depuis une commande d'achat envoyée
     */
    public FactureFournisseur createFactureFromCommandeAchat(CommandeAchat commande, Utilisateur createur) {
        // Vérifier si une facture existe déjà pour cette commande
        Optional<FactureFournisseur> existing = factureFournisseurRepository.findByCommandeAchatId(commande.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        
        String refInterne = generateFactureFournisseurNumero();
        
        FactureFournisseur facture = FactureFournisseur.builder()
            .refInterne(refInterne)
            .refFournisseur("FF-" + commande.getNumero()) // Référence temporaire, à modifier par l'utilisateur
            .fournisseur(commande.getFournisseur())
            .commandeAchat(commande)
            .montantHT(commande.getTotalHT())
            .montantTTC(commande.getTotalTTC())
            .devise(commande.getDevise())
            .statutCode("A_PAYER")
            .dateFacture(LocalDate.now())
            .dateEcheance(LocalDate.now().plusDays(30))
            .build();
        
        FactureFournisseur saved = factureFournisseurRepository.save(facture);
        
        auditService.logAction("FACTURE_FOURNISSEUR", saved.getId(), "CREATION_AUTO_CMD", createur,
            Map.of("commandeAchatId", commande.getId().toString(), "commandeNumero", commande.getNumero()));
        
        return saved;
    }
    
    public synchronized String generateFactureFournisseurNumero() {
        String prefix = "FF-" + LocalDate.now().getYear() + "-";
        Integer maxNum = factureFournisseurRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%05d", nextNum);
            if (factureFournisseurRepository.findByRefInterne(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    // ============ ENCAISSEMENTS ============
    
    public EncaissementClient enregistrerEncaissement(EncaissementClient encaissement, Utilisateur encaisseur) {
        FactureClient facture = encaissement.getFacture();
        
        // RÈGLE CRITIQUE: Vérifier la séparation des tâches
        // L'encaisseur ne doit PAS être le commercial qui a créé la commande
        if (facture.getCommande() != null && facture.getCommande().getCommercial() != null) {
            if (facture.getCommande().getCommercial().getId().equals(encaisseur.getId())) {
                throw new RuntimeException("SÉPARATION DES TÂCHES: Le commercial qui a créé la commande ne peut pas encaisser le paiement");
            }
        }
        
        encaissement.setEncaisseur(encaisseur);
        EncaissementClient saved = encaissementClientRepository.save(encaissement);
        
        // Mettre à jour le montant encaissé
        facture.setMontantEncaisse(facture.getMontantEncaisse().add(encaissement.getMontant()));
        
        // Mettre à jour le statut
        if (facture.getMontantRestant().compareTo(BigDecimal.ZERO) <= 0) {
            facture.setStatutCode("PAYEE");
        } else {
            facture.setStatutCode("PAYEE_PARTIEL");
        }
        factureClientRepository.save(facture);
        
        auditService.logAction("ENCAISSEMENT_CLIENT", saved.getId(), "CREATION", encaisseur, null);
        return saved;
    }
    
    // ============ STATISTIQUES ============
    
    public BigDecimal getTotalFacturesFournisseurImpayees() {
        BigDecimal total = factureFournisseurRepository.getTotalOutstandingAmount();
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalFacturesClientImpayees() {
        BigDecimal total = factureClientRepository.getTotalOutstandingAmount();
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public List<FactureFournisseur> getFacturesFournisseurEnRetard() {
        return factureFournisseurRepository.findOverdueFactures(LocalDate.now());
    }
    
    public List<FactureClient> getFacturesClientEnRetard() {
        return factureClientRepository.findOverdueFactures(LocalDate.now());
    }
    
    // ============ STATISTIQUES AVANCÉES ============
    
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        // Créances et dettes
        BigDecimal creancesClient = factureClientRepository.getTotalOutstandingAmount();
        BigDecimal dettesFournisseur = factureFournisseurRepository.getTotalOutstandingAmount();
        
        stats.put("creancesClient", creancesClient != null ? creancesClient : BigDecimal.ZERO);
        stats.put("dettesFournisseur", dettesFournisseur != null ? dettesFournisseur : BigDecimal.ZERO);
        
        // Factures en retard
        stats.put("facturesClientRetard", factureClientRepository.findOverdueFactures(today));
        stats.put("facturesFournisseurRetard", factureFournisseurRepository.findOverdueFactures(today));
        
        // Compteurs par statut
        stats.put("facturesClientAPayer", factureClientRepository.countByStatut("A_PAYER"));
        stats.put("facturesClientPartielles", factureClientRepository.countByStatut("PAYEE_PARTIEL"));
        stats.put("facturesClientPayees", factureClientRepository.countByStatut("PAYEE"));
        
        stats.put("facturesFournisseurAPayer", factureFournisseurRepository.countByStatut("A_PAYER"));
        stats.put("facturesFournisseurPartielles", factureFournisseurRepository.countByStatut("PAYEE_PARTIEL"));
        stats.put("facturesFournisseurPayees", factureFournisseurRepository.countByStatut("PAYEE"));
        
        // CA du mois
        BigDecimal caClient = factureClientRepository.sumByPeriod(startOfMonth, today);
        BigDecimal achats = factureFournisseurRepository.sumByPeriod(startOfMonth, today);
        stats.put("caClientMois", caClient != null ? caClient : BigDecimal.ZERO);
        stats.put("achatsMois", achats != null ? achats : BigDecimal.ZERO);
        
        // Encaissements et paiements du mois
        BigDecimal encaissementsMois = encaissementClientRepository.sumEncaissementsByPeriod(startOfMonth, today);
        BigDecimal paiementsMois = paiementFournisseurRepository.sumPaiementsByPeriod(startOfMonth, today);
        stats.put("encaissementsMois", encaissementsMois != null ? encaissementsMois : BigDecimal.ZERO);
        stats.put("paiementsMois", paiementsMois != null ? paiementsMois : BigDecimal.ZERO);
        
        // Factures récentes
        stats.put("facturesClientRecentes", factureClientRepository.findRecentFactures(startOfMonth));
        stats.put("facturesFournisseurRecentes", factureFournisseurRepository.findRecentFactures(startOfMonth));
        
        return stats;
    }
    
    // ============ DÉTAILS FACTURES ============
    
    public Optional<FactureClient> findFactureClientByIdWithDetails(UUID id) {
        return factureClientRepository.findByIdWithDetails(id);
    }
    
    public Optional<FactureFournisseur> findFactureFournisseurByIdWithDetails(UUID id) {
        return factureFournisseurRepository.findByIdWithDetails(id);
    }
    
    public List<EncaissementClient> findEncaissementsByFactureClient(UUID factureId) {
        return encaissementClientRepository.findByFactureId(factureId);
    }
    
    public List<PaiementFournisseur> findPaiementsByFactureFournisseur(UUID factureId) {
        return paiementFournisseurRepository.findByFactureId(factureId);
    }
}
