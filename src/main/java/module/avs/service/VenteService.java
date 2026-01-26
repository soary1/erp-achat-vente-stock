package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.*;
import module.avs.model.tiers.Client;
import module.avs.model.vente.*;
import module.avs.repository.stock.*;
import module.avs.repository.vente.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class VenteService {
    
    private final DevisClientRepository devisClientRepository;
    private final LigneDevisClientRepository ligneDevisClientRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final LigneCommandeClientRepository ligneCommandeClientRepository;
    private final ReservationStockRepository reservationStockRepository;
    private final BonLivraisonRepository bonLivraisonRepository;
    private final LigneBonLivraisonRepository ligneBonLivraisonRepository;
    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final TypeMouvementRepository typeMouvementRepository;
    private final AuditService auditService;
    private final UtilisateurService utilisateurService;
    
    // ============ DEVIS ============
    
    public List<DevisClient> findAllDevis() {
        return devisClientRepository.findAll();
    }
    
    public Page<DevisClient> findAllDevis(Pageable pageable) {
        return devisClientRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Optional<DevisClient> findDevisById(UUID id) {
        return devisClientRepository.findById(id);
    }
    
    public String generateDevisNumero() {
        String prefix = "DEV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = devisClientRepository.findMaxNumero(prefix + "%");
        return prefix + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);
    }
    
    public DevisClient createDevis(DevisClient devis, Utilisateur createur) {
        devis.setNumero(generateDevisNumero());
        devis.setStatutCode("BROUILLON");
        devis.recalculerTotaux();
        DevisClient saved = devisClientRepository.save(devis);
        
        auditService.logAction("DEVIS_CLIENT", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public DevisClient saveDevis(DevisClient devis) {
        devis.recalculerTotaux();
        return devisClientRepository.save(devis);
    }
    
    public DevisClient validerDevis(UUID devisId, Utilisateur acteur) {
        DevisClient devis = devisClientRepository.findById(devisId)
            .orElseThrow(() -> new RuntimeException("Devis non trouvé"));
        
        devis.setStatutCode("VALIDE");
        DevisClient saved = devisClientRepository.save(devis);
        
        auditService.logWorkflow("DEVIS_CLIENT", devisId, "BROUILLON", "VALIDE", acteur, "VALIDATION", null);
        return saved;
    }
    
    // ============ COMMANDES CLIENT ============
    
    public List<CommandeClient> findAllCommandesClient() {
        return commandeClientRepository.findAll();
    }
    
    public Page<CommandeClient> findAllCommandesClient(Pageable pageable) {
        return commandeClientRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Optional<CommandeClient> findCommandeClientById(UUID id) {
        return commandeClientRepository.findById(id);
    }
    
    public List<CommandeClient> findCommandesByStatut(String statut) {
        return commandeClientRepository.findByStatutCode(statut);
    }
    
    public String generateCommandeClientNumero() {
        String prefix = "CMD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = commandeClientRepository.findMaxNumero(prefix + "%");
        return prefix + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);
    }
    
    public CommandeClient createCommandeClient(CommandeClient commande, Utilisateur createur) {
        commande.setNumero(generateCommandeClientNumero());
        commande.setStatutCode("BROUILLON");
        commande.setCommercial(createur);
        commande.recalculerTotaux();
        CommandeClient saved = commandeClientRepository.save(commande);
        
        auditService.logAction("COMMANDE_CLIENT", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public CommandeClient createCommandeFromDevis(UUID devisId, Utilisateur createur) {
        DevisClient devis = devisClientRepository.findById(devisId)
            .orElseThrow(() -> new RuntimeException("Devis non trouvé"));
        
        CommandeClient commande = CommandeClient.builder()
            .devis(devis)
            .client(devis.getClient())
            .site(devis.getSite())
            .commercial(createur)
            .statutCode("BROUILLON")
            .totalHT(devis.getTotalHT())
            .totalTTC(devis.getTotalTTC())
            .build();
        
        commande.setNumero(generateCommandeClientNumero());
        CommandeClient saved = commandeClientRepository.save(commande);
        
        // Copier les lignes du devis
        for (LigneDevisClient ligneDevis : devis.getLignes()) {
            LigneCommandeClient ligneCmd = LigneCommandeClient.builder()
                .commande(saved)
                .article(ligneDevis.getArticle())
                .qtyOrdered(ligneDevis.getQty())
                .priceUnit(ligneDevis.getPriceUnit())
                .remisePct(ligneDevis.getRemisePct())
                .build();
            ligneCommandeClientRepository.save(ligneCmd);
        }
        
        devis.setStatutCode("TRANSFORME");
        devisClientRepository.save(devis);
        
        auditService.logAction("COMMANDE_CLIENT", saved.getId(), "CREATION_DEPUIS_DEVIS", createur, null);
        return saved;
    }
    
    public CommandeClient saveCommandeClient(CommandeClient commande) {
        commande.recalculerTotaux();
        return commandeClientRepository.save(commande);
    }
    
    public CommandeClient confirmerCommande(UUID commandeId, Utilisateur acteur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        // Réserver le stock pour chaque ligne
        for (LigneCommandeClient ligne : commande.getLignes()) {
            reserverStockPourLigne(ligne);
        }
        
        commande.setStatutCode("CONFIRMEE");
        CommandeClient saved = commandeClientRepository.save(commande);
        
        auditService.logWorkflow("COMMANDE_CLIENT", commandeId, "BROUILLON", "CONFIRMEE", acteur, "CONFIRMATION", null);
        return saved;
    }
    
    private void reserverStockPourLigne(LigneCommandeClient ligne) {
        List<Stock> stocksDispo = stockRepository.findAvailableStockFEFO(ligne.getArticle().getId());
        BigDecimal qtyAReserver = ligne.getQtyOrdered();
        
        for (Stock stock : stocksDispo) {
            if (qtyAReserver.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal disponible = stock.getQtyDisponible();
            BigDecimal aReserver = disponible.min(qtyAReserver);
            
            if (aReserver.compareTo(BigDecimal.ZERO) > 0) {
                // Créer la réservation
                ReservationStock reservation = ReservationStock.builder()
                    .ligneCommande(ligne)
                    .article(ligne.getArticle())
                    .depot(stock.getDepot())
                    .lot(stock.getLot())
                    .qtyReservee(aReserver)
                    .build();
                reservationStockRepository.save(reservation);
                
                // Mettre à jour le stock réservé
                stock.setQtyReserve(stock.getQtyReserve().add(aReserver));
                stockRepository.save(stock);
                
                qtyAReserver = qtyAReserver.subtract(aReserver);
            }
        }
        
        if (qtyAReserver.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Stock insuffisant pour l'article " + ligne.getArticle().getSku());
        }
    }
    
    public CommandeClient preparerCommande(UUID commandeId, Utilisateur acteur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        commande.setStatutCode("PREPARATION");
        CommandeClient saved = commandeClientRepository.save(commande);
        
        auditService.logWorkflow("COMMANDE_CLIENT", commandeId, "CONFIRMEE", "PREPARATION", acteur, "PREPARATION", null);
        return saved;
    }
    
    // ============ LIVRAISONS ============
    
    public List<BonLivraison> findAllLivraisons() {
        return bonLivraisonRepository.findAll();
    }
    
    public Page<BonLivraison> findAllLivraisons(Pageable pageable) {
        return bonLivraisonRepository.findAllByOrderByDateExpeditionDesc(pageable);
    }
    
    public Optional<BonLivraison> findLivraisonById(UUID id) {
        return bonLivraisonRepository.findById(id);
    }
    
    public String generateLivraisonNumero() {
        String prefix = "BL-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = bonLivraisonRepository.findMaxNumero(prefix + "%");
        return prefix + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);
    }
    
    public BonLivraison createLivraison(UUID commandeId, Utilisateur createur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        BonLivraison bl = BonLivraison.builder()
            .numero(generateLivraisonNumero())
            .commande(commande)
            .statutCode("BROUILLON")
            .dateExpedition(OffsetDateTime.now())
            .build();
        
        BonLivraison saved = bonLivraisonRepository.save(bl);
        
        auditService.logAction("BON_LIVRAISON", saved.getId(), "CREATION", createur, null);
        return saved;
    }
    
    public BonLivraison validerLivraison(UUID livraisonId, Utilisateur acteur) {
        BonLivraison livraison = bonLivraisonRepository.findById(livraisonId)
            .orElseThrow(() -> new RuntimeException("Livraison non trouvée"));
        
        TypeMouvement typeMouvement = typeMouvementRepository.findById("EXPEDITION")
            .orElseThrow(() -> new RuntimeException("Type mouvement non trouvé"));
        
        // Décrémenter le stock et libérer les réservations
        for (LigneBonLivraison ligne : livraison.getLignes()) {
            // Trouver les réservations associées
            LigneCommandeClient ligneCmd = livraison.getCommande().getLignes().stream()
                .filter(l -> l.getArticle().getId().equals(ligne.getArticle().getId()))
                .findFirst()
                .orElse(null);
            
            if (ligneCmd != null) {
                List<ReservationStock> reservations = reservationStockRepository.findByLigneCommandeId(ligneCmd.getId());
                BigDecimal qtyALivrer = ligne.getQtyLivree();
                
                for (ReservationStock reservation : reservations) {
                    if (qtyALivrer.compareTo(BigDecimal.ZERO) <= 0) break;
                    
                    BigDecimal aDeduire = reservation.getQtyReservee().min(qtyALivrer);
                    
                    // Mettre à jour le stock
                    Optional<Stock> stockOpt = stockRepository.findByDepotIdAndArticleIdAndLotId(
                        reservation.getDepot().getId(),
                        reservation.getArticle().getId(),
                        reservation.getLot() != null ? reservation.getLot().getId() : null
                    );
                    
                    if (stockOpt.isPresent()) {
                        Stock stock = stockOpt.get();
                        stock.setQtyReel(stock.getQtyReel().subtract(aDeduire));
                        stock.setQtyReserve(stock.getQtyReserve().subtract(aDeduire));
                        stockRepository.save(stock);
                        
                        // Créer le mouvement de sortie
                        MouvementStock mouvement = MouvementStock.builder()
                            .typeMouvement(typeMouvement)
                            .referenceDoc(livraison.getNumero())
                            .article(ligne.getArticle())
                            .lot(ligne.getLot())
                            .depotSource(stock.getDepot())
                            .emplacementSource(stock.getEmplacement())
                            .qty(aDeduire)
                            .utilisateur(acteur)
                            .createdAt(OffsetDateTime.now())
                            .build();
                        mouvementStockRepository.save(mouvement);
                    }
                    
                    qtyALivrer = qtyALivrer.subtract(aDeduire);
                }
                
                // Mettre à jour la quantité livrée sur la commande
                ligneCmd.setQtyDelivered(ligneCmd.getQtyDelivered().add(ligne.getQtyLivree()));
                ligneCommandeClientRepository.save(ligneCmd);
            }
        }
        
        livraison.setStatutCode("VALIDE");
        BonLivraison saved = bonLivraisonRepository.save(livraison);
        
        // Mettre à jour le statut de la commande
        updateStatutCommandeApresLivraison(livraison.getCommande().getId());
        
        auditService.logWorkflow("BON_LIVRAISON", livraisonId, "BROUILLON", "VALIDE", acteur, "VALIDATION", null);
        return saved;
    }
    
    private void updateStatutCommandeApresLivraison(UUID commandeId) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        boolean toutLivre = commande.getLignes().stream()
            .allMatch(l -> l.getQtyRestante().compareTo(BigDecimal.ZERO) <= 0);
        
        if (toutLivre) {
            commande.setStatutCode("EXPEDIEE");
            commandeClientRepository.save(commande);
        }
    }
    
    // ============ VALIDATION REMISES ============
    
    public boolean validerRemise(BigDecimal remisePct, BigDecimal plafondRemise, UUID validateurId) {
        if (remisePct.compareTo(plafondRemise) <= 0) {
            return true;
        }
        // Vérifier si le validateur a le droit d'accorder cette remise
        return utilisateurService.hasRoleWithDelegation(validateurId, "MANAGER");
    }
}
