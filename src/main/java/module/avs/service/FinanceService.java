package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.achat.CommandeAchat;
import module.avs.model.finance.*;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.BonReception;
import module.avs.model.vente.CommandeClient;
import module.avs.repository.finance.*;
import module.avs.repository.stock.BonReceptionRepository;
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
    
    public String generateFactureClientNumero() {
        return "FAC-" + LocalDate.now().getYear() + "-" + 
               String.format("%05d", factureClientRepository.count() + 1);
    }
    
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
            .build();
        
        FactureClient saved = factureClientRepository.save(facture);
        
        auditService.logAction("FACTURE_CLIENT", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    // ============ ENCAISSEMENTS ============
    
    public EncaissementClient enregistrerEncaissement(EncaissementClient encaissement, Utilisateur acteur) {
        FactureClient facture = encaissement.getFacture();
        
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
        
        auditService.logAction("ENCAISSEMENT_CLIENT", saved.getId(), "CREATION", acteur, null);
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
}
