package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.article.Article;
import module.avs.model.organisation.*;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.*;
import module.avs.model.achat.*;
import module.avs.repository.stock.*;
import module.avs.repository.achat.*;
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
public class StockService {
    
    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final TypeMouvementRepository typeMouvementRepository;
    private final LotRepository lotRepository;
    private final BonReceptionRepository bonReceptionRepository;
    private final LigneBonReceptionRepository ligneBonReceptionRepository;
    private final LigneCommandeAchatRepository ligneCommandeAchatRepository;
    private final AuditService auditService;
    private final AchatService achatService;
    
    // ============ GESTION DU STOCK ============
    
    public List<Stock> findStockByArticle(UUID articleId) {
        return stockRepository.findByArticleId(articleId);
    }
    
    public List<Stock> findStockByDepot(UUID depotId) {
        return stockRepository.findByDepotId(depotId);
    }
    
    public BigDecimal getStockTotal(UUID articleId) {
        BigDecimal total = stockRepository.getTotalStockByArticle(articleId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public BigDecimal getStockDisponible(UUID articleId) {
        List<Stock> stocks = stockRepository.findAvailableStockByArticle(articleId);
        return stocks.stream()
            .map(Stock::getQtyDisponible)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Mise à jour du stock (entrée ou sortie)
    public Stock updateStock(Depot depot, Emplacement emplacement, Article article, 
                            Lot lot, BigDecimal qty, BigDecimal unitCost,
                            TypeMouvement typeMouvement, String referenceDoc, Utilisateur user) {
        
        // Recherche ou création du stock
        Optional<Stock> existingStock = emplacement != null 
            ? stockRepository.findByDepotIdAndEmplacementIdAndArticleIdAndLotId(
                depot.getId(), emplacement.getId(), article.getId(), lot != null ? lot.getId() : null)
            : stockRepository.findByDepotIdAndArticleIdAndLotId(
                depot.getId(), article.getId(), lot != null ? lot.getId() : null);
        
        Stock stock;
        if (existingStock.isPresent()) {
            stock = existingStock.get();
            stock.setQtyReel(stock.getQtyReel().add(qty.multiply(BigDecimal.valueOf(typeMouvement.getSens()))));
        } else {
            stock = Stock.builder()
                .depot(depot)
                .emplacement(emplacement)
                .article(article)
                .lot(lot)
                .qtyReel(qty.multiply(BigDecimal.valueOf(typeMouvement.getSens())))
                .qtyReserve(BigDecimal.ZERO)
                .build();
        }
        
        stock = stockRepository.save(stock);
        
        // Création du mouvement
        MouvementStock mouvement = MouvementStock.builder()
            .typeMouvement(typeMouvement)
            .referenceDoc(referenceDoc)
            .article(article)
            .lot(lot)
            .qty(qty)
            .unitCost(unitCost)
            .utilisateur(user)
            .createdAt(OffsetDateTime.now())
            .build();
        
        if (typeMouvement.getSens() > 0) {
            mouvement.setDepotDest(depot);
            mouvement.setEmplacementDest(emplacement);
        } else {
            mouvement.setDepotSource(depot);
            mouvement.setEmplacementSource(emplacement);
        }
        
        mouvementStockRepository.save(mouvement);
        
        return stock;
    }
    
    // Réservation de stock
    public void reserverStock(UUID stockId, BigDecimal qty) {
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> new RuntimeException("Stock non trouvé"));
        
        if (stock.getQtyDisponible().compareTo(qty) < 0) {
            throw new RuntimeException("Stock insuffisant pour la réservation");
        }
        
        stock.setQtyReserve(stock.getQtyReserve().add(qty));
        stockRepository.save(stock);
    }
    
    public void libererReservation(UUID stockId, BigDecimal qty) {
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> new RuntimeException("Stock non trouvé"));
        
        stock.setQtyReserve(stock.getQtyReserve().subtract(qty));
        if (stock.getQtyReserve().compareTo(BigDecimal.ZERO) < 0) {
            stock.setQtyReserve(BigDecimal.ZERO);
        }
        stockRepository.save(stock);
    }
    
    // Allocation FIFO/FEFO
    public List<Stock> getAllocationFEFO(UUID articleId, BigDecimal qtyNeeded) {
        List<Stock> availableStocks = stockRepository.findAvailableStockFEFO(articleId);
        List<Stock> allocated = new ArrayList<>();
        BigDecimal remaining = qtyNeeded;
        
        for (Stock stock : availableStocks) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal available = stock.getQtyDisponible();
            if (available.compareTo(BigDecimal.ZERO) > 0) {
                allocated.add(stock);
                remaining = remaining.subtract(available);
            }
        }
        
        return allocated;
    }
    
    // ============ LOTS ============
    
    public List<Lot> findLotsByArticle(UUID articleId) {
        return lotRepository.findByArticleId(articleId);
    }
    
    public Optional<Lot> findLotById(UUID id) {
        return lotRepository.findById(id);
    }
    
    public Lot createLot(Lot lot) {
        // Vérification de péremption
        if (lot.getDatePeremption() != null && lot.getDatePeremption().isBefore(LocalDate.now())) {
            lot.setStatutQualiteCode("REJETE");
        } else {
            lot.setStatutQualiteCode("CONFORME");
        }
        return lotRepository.save(lot);
    }
    
    public List<Lot> findExpiredLots() {
        return lotRepository.findExpiredLots(LocalDate.now());
    }
    
    public List<Lot> findLotsExpiringSoon(int daysAhead) {
        return lotRepository.findLotsExpiringSoon(LocalDate.now(), LocalDate.now().plusDays(daysAhead));
    }
    
    // ============ RÉCEPTIONS ============
    
    public List<BonReception> findAllReceptions() {
        return bonReceptionRepository.findAll();
    }
    
    public Page<BonReception> findAllReceptions(Pageable pageable) {
        return bonReceptionRepository.findAllByOrderByDateReceptionDesc(pageable);
    }
    
    public Optional<BonReception> findReceptionById(UUID id) {
        return bonReceptionRepository.findById(id);
    }
    
    public List<BonReception> findReceptionsByCommande(UUID commandeId) {
        return bonReceptionRepository.findByCommandeAchatId(commandeId);
    }
    
    public String generateReceptionNumero() {
        String prefix = "BR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = bonReceptionRepository.findMaxNumero(prefix + "%");
        return prefix + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);
    }
    
    public BonReception createReception(BonReception reception, Utilisateur user) {
        reception.setNumero(generateReceptionNumero());
        reception.setStatutCode("BROUILLON");
        reception.setDateReception(OffsetDateTime.now());
        BonReception saved = bonReceptionRepository.save(reception);
        
        auditService.logAction("BON_RECEPTION", saved.getId(), "CREATION", user, null);
        return saved;
    }
    
    public BonReception validerReception(UUID receptionId, Utilisateur user) {
        BonReception reception = bonReceptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("Réception non trouvée"));
        
        TypeMouvement typeMouvement = typeMouvementRepository.findById("RECEPTION")
            .orElseThrow(() -> new RuntimeException("Type mouvement non trouvé"));
        
        // Mise à jour du stock pour chaque ligne
        for (LigneBonReception ligne : reception.getLignes()) {
            updateStock(
                reception.getDepot(),
                ligne.getEmplacement(),
                ligne.getArticle(),
                ligne.getLot(),
                ligne.getQtyReceived(),
                ligne.getUnitCost(),
                typeMouvement,
                reception.getNumero(),
                user
            );
            
            // Mise à jour de la quantité reçue sur la commande
            if (reception.getCommandeAchat() != null) {
                reception.getCommandeAchat().getLignes().stream()
                    .filter(l -> l.getArticle().getId().equals(ligne.getArticle().getId()))
                    .findFirst()
                    .ifPresent(ligneCmd -> {
                        ligneCmd.setQtyReceived(ligneCmd.getQtyReceived().add(ligne.getQtyReceived()));
                        ligneCommandeAchatRepository.save(ligneCmd);
                    });
            }
        }
        
        reception.setStatutCode("VALIDE");
        BonReception saved = bonReceptionRepository.save(reception);
        
        // Mise à jour du statut de la commande
        if (reception.getCommandeAchat() != null) {
            achatService.updateStatutCommandeApresReception(reception.getCommandeAchat().getId());
        }
        
        auditService.logWorkflow("BON_RECEPTION", receptionId, "BROUILLON", "VALIDE", user, "VALIDATION", null);
        return saved;
    }
    
    // ============ MOUVEMENTS ============
    
    public Page<MouvementStock> findAllMouvements(Pageable pageable) {
        return mouvementStockRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public List<MouvementStock> findMouvementsByArticle(UUID articleId) {
        return mouvementStockRepository.findByArticleIdOrderByCreatedAtDesc(articleId);
    }
    
    // Transfert entre dépôts
    public void transfererStock(Depot depotSource, Emplacement empSource,
                               Depot depotDest, Emplacement empDest,
                               Article article, Lot lot, BigDecimal qty,
                               Utilisateur user) {
        
        TypeMouvement typeTransfert = typeMouvementRepository.findById("TRANSFERT")
            .orElseThrow(() -> new RuntimeException("Type mouvement non trouvé"));
        
        // Vérifier disponibilité
        Optional<Stock> stockSource = stockRepository.findByDepotIdAndEmplacementIdAndArticleIdAndLotId(
            depotSource.getId(), empSource != null ? empSource.getId() : null, 
            article.getId(), lot != null ? lot.getId() : null);
        
        if (stockSource.isEmpty() || stockSource.get().getQtyDisponible().compareTo(qty) < 0) {
            throw new RuntimeException("Stock insuffisant pour le transfert");
        }
        
        // Sortie du dépôt source
        Stock source = stockSource.get();
        source.setQtyReel(source.getQtyReel().subtract(qty));
        stockRepository.save(source);
        
        // Entrée dans le dépôt destination
        Optional<Stock> stockDest = stockRepository.findByDepotIdAndEmplacementIdAndArticleIdAndLotId(
            depotDest.getId(), empDest != null ? empDest.getId() : null,
            article.getId(), lot != null ? lot.getId() : null);
        
        Stock dest;
        if (stockDest.isPresent()) {
            dest = stockDest.get();
            dest.setQtyReel(dest.getQtyReel().add(qty));
        } else {
            dest = Stock.builder()
                .depot(depotDest)
                .emplacement(empDest)
                .article(article)
                .lot(lot)
                .qtyReel(qty)
                .qtyReserve(BigDecimal.ZERO)
                .build();
        }
        stockRepository.save(dest);
        
        // Mouvement de transfert
        MouvementStock mouvement = MouvementStock.builder()
            .typeMouvement(typeTransfert)
            .referenceDoc("TRF-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
            .article(article)
            .lot(lot)
            .depotSource(depotSource)
            .emplacementSource(empSource)
            .depotDest(depotDest)
            .emplacementDest(empDest)
            .qty(qty)
            .utilisateur(user)
            .createdAt(OffsetDateTime.now())
            .build();
        
        mouvementStockRepository.save(mouvement);
    }
}
