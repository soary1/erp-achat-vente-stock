package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.article.Article;
import module.avs.model.organisation.Emplacement;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.Lot;
import module.avs.model.stock.Stock;
import module.avs.model.vente.*;
import module.avs.repository.stock.LotRepository;
import module.avs.repository.stock.StockRepository;
import module.avs.repository.vente.*;
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
public class PickingService {
    
    private final OrdrePreparationRepository ordrePreparationRepository;
    private final LigneOrdrePreparationRepository ligneOrdrePreparationRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final ReservationStockRepository reservationStockRepository;
    private final StockRepository stockRepository;
    private final LotRepository lotRepository;
    private final AuditService auditService;
    
    /**
     * Génère un ordre de préparation à partir d'une commande confirmée
     */
    public OrdrePreparation genererOrdrePreparation(UUID commandeId, Utilisateur createur) {
        CommandeClient commande = commandeClientRepository.findById(commandeId)
            .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        
        if (!"CONFIRMEE".equals(commande.getStatutCode())) {
            throw new RuntimeException("Seules les commandes confirmées peuvent être préparées");
        }
        
        OrdrePreparation ordre = OrdrePreparation.builder()
            .numero(generateOrdreNumero())
            .commande(commande)
            .statutCode("EN_ATTENTE")
            .build();
        
        OrdrePreparation savedOrdre = ordrePreparationRepository.save(ordre);
        
        // Créer les lignes avec allocation FIFO
        for (LigneCommandeClient ligneCmd : commande.getLignes()) {
            allouerLigneAvecFIFO(savedOrdre, ligneCmd);
        }
        
        auditService.logAction("ORDRE_PREPARATION", savedOrdre.getId(), "CREATION", createur, null);
        return savedOrdre;
    }
    
    /**
     * Alloue une ligne de commande en respectant le FIFO strict
     */
    private void allouerLigneAvecFIFO(OrdrePreparation ordre, LigneCommandeClient ligneCmd) {
        // Récupérer les réservations pour cette ligne (déjà allouées par FIFO lors de la confirmation)
        List<ReservationStock> reservations = reservationStockRepository.findByLigneCommandeId(ligneCmd.getId());
        
        for (ReservationStock reservation : reservations) {
            // Trouver le stock correspondant pour récupérer l'emplacement
            Optional<Stock> stockOpt = stockRepository.findByDepotIdAndArticleIdAndLotId(
                reservation.getDepot().getId(),
                reservation.getArticle().getId(),
                reservation.getLot() != null ? reservation.getLot().getId() : null
            );
            
            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                
                LigneOrdrePreparation ligne = LigneOrdrePreparation.builder()
                    .ordre(ordre)
                    .ligneCommande(ligneCmd)
                    .article(reservation.getArticle())
                    .lot(reservation.getLot())
                    .emplacement(stock.getEmplacement())
                    .qtyAPreparer(reservation.getQtyReservee())
                    .build();
                
                ligneOrdrePreparationRepository.save(ligne);
            }
        }
    }
    
    /**
     * Démarre la préparation d'un ordre
     */
    public OrdrePreparation demarrerPreparation(UUID ordreId, Utilisateur preparateur) {
        OrdrePreparation ordre = ordrePreparationRepository.findById(ordreId)
            .orElseThrow(() -> new RuntimeException("Ordre de préparation non trouvé"));
        
        if (!"EN_ATTENTE".equals(ordre.getStatutCode())) {
            throw new RuntimeException("Cet ordre ne peut pas être démarré");
        }
        
        ordre.setStatutCode("EN_COURS");
        ordre.setPreparateur(preparateur);
        ordre.setDateDebutPreparation(OffsetDateTime.now());
        
        OrdrePreparation saved = ordrePreparationRepository.save(ordre);
        auditService.logWorkflow("ORDRE_PREPARATION", ordreId, "EN_ATTENTE", "EN_COURS", preparateur, "DEMARRAGE", null);
        
        return saved;
    }
    
    /**
     * Scan d'un article lors du picking
     * Vérifie que le lot scanné correspond au lot FIFO alloué
     */
    public ScanResult scanArticle(UUID ligneOrdreId, String lotNumero, BigDecimal qtyScan, Utilisateur preparateur) {
        LigneOrdrePreparation ligne = ligneOrdrePreparationRepository.findById(ligneOrdreId)
            .orElseThrow(() -> new RuntimeException("Ligne de préparation non trouvée"));
        
        // Vérifier que le lot scanné est bien celui attendu (FIFO)
        String lotAttendu = ligne.getLot() != null ? ligne.getLot().getNumeroLot() : null;
        
        boolean forcageFifo = false;
        String message = "Article scanné avec succès";
        
        if (lotAttendu != null && !lotAttendu.equals(lotNumero)) {
            // Lot différent du FIFO attendu
            // Dans un système strict, on bloquerait ici
            // Mais on peut permettre le forçage avec validation
            return new ScanResult(false, "ERREUR FIFO: Lot attendu = " + lotAttendu + ", Lot scanné = " + lotNumero, true);
        }
        
        // Mise à jour de la quantité préparée
        ligne.setQtyPreparee(ligne.getQtyPreparee().add(qtyScan));
        ligne.setScanne(true);
        ligne.setDateScan(OffsetDateTime.now());
        ligne.setForcageFifo(forcageFifo);
        
        ligneOrdrePreparationRepository.save(ligne);
        
        // Vérifier si l'ordre est terminé
        OrdrePreparation ordre = ligne.getOrdre();
        if (ordre.isTermine()) {
            terminerOrdre(ordre.getId(), preparateur);
        }
        
        return new ScanResult(true, message, false);
    }
    
    /**
     * Force le scan d'un lot différent (nécessite validation supérieur)
     */
    public void forcerScanLotDifferent(UUID ligneOrdreId, String lotNumero, BigDecimal qtyScan, 
                                        Utilisateur preparateur, Utilisateur validateur, String motif) {
        LigneOrdrePreparation ligne = ligneOrdrePreparationRepository.findById(ligneOrdreId)
            .orElseThrow(() -> new RuntimeException("Ligne de préparation non trouvée"));
        
        ligne.setQtyPreparee(ligne.getQtyPreparee().add(qtyScan));
        ligne.setScanne(true);
        ligne.setDateScan(OffsetDateTime.now());
        ligne.setForcageFifo(true);
        ligne.setForcageValidateur(validateur);
        ligne.setForcageMotif(motif);
        
        ligneOrdrePreparationRepository.save(ligne);
        
        auditService.logAction("LIGNE_ORDRE_PREP", ligneOrdreId, "FORCAGE_FIFO", validateur, 
            "Forcage lot: " + lotNumero + " - Motif: " + motif);
    }
    
    /**
     * Termine un ordre de préparation
     */
    public OrdrePreparation terminerOrdre(UUID ordreId, Utilisateur preparateur) {
        OrdrePreparation ordre = ordrePreparationRepository.findById(ordreId)
            .orElseThrow(() -> new RuntimeException("Ordre de préparation non trouvé"));
        
        if (!ordre.isTermine()) {
            throw new RuntimeException("Toutes les lignes ne sont pas encore préparées");
        }
        
        ordre.setStatutCode("TERMINE");
        ordre.setDateFinPreparation(OffsetDateTime.now());
        
        // Mettre à jour le statut de la commande
        CommandeClient commande = ordre.getCommande();
        commande.setStatutCode("PREPARATION");
        commandeClientRepository.save(commande);
        
        OrdrePreparation saved = ordrePreparationRepository.save(ordre);
        auditService.logWorkflow("ORDRE_PREPARATION", ordreId, "EN_COURS", "TERMINE", preparateur, "CLOTURE", null);
        
        return saved;
    }
    
    /**
     * Récupère les ordres en attente par priorité
     */
    public List<OrdrePreparation> getOrdresEnAttente() {
        return ordrePreparationRepository.findByStatutCodeOrderByPrioriteDescDateCreationAsc("EN_ATTENTE");
    }
    
    /**
     * Récupère les ordres d'un préparateur
     */
    public List<OrdrePreparation> getOrdresPreparateur(UUID preparateurId) {
        return ordrePreparationRepository.findByPreparateurId(preparateurId);
    }
    
    private synchronized String generateOrdreNumero() {
        String prefix = "OP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = ordrePreparationRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%04d", nextNum);
            if (ordrePreparationRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    /**
     * Résultat d'un scan
     */
    public record ScanResult(
        boolean success,
        String message,
        boolean requiresForcage
    ) {}
}
