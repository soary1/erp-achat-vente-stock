package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.dto.BonReceptionDTO;
import module.avs.dto.StockTransfertDTO;
import module.avs.model.article.Article;
import module.avs.model.organisation.*;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.*;
import module.avs.model.achat.*;
import module.avs.repository.stock.*;
import module.avs.repository.achat.*;
import module.avs.repository.article.ArticleRepository;
import module.avs.repository.organisation.DepotRepository;
import module.avs.repository.organisation.EmplacementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ControleQualiteRepository controleQualiteRepository;
    private final AuditService auditService;
    private final AchatService achatService;
    private final ArticleRepository articleRepository;
    private final DepotRepository depotRepository;
    private final EmplacementRepository emplacementRepository;
    private final CommandeAchatRepository commandeAchatRepository;
    
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
    
    // ============ NUMÉROTATION MOUVEMENTS ============
    
    public synchronized String generateMouvementNumero() {
        String prefix = "MVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = mouvementStockRepository.findMaxNumero(prefix);
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%05d", nextNum);
            if (mouvementStockRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    // ============ CRÉATION DE MOUVEMENT ============
    
    private MouvementStock creerMouvement(TypeMouvement typeMouvement, String referenceDoc,
                                          Article article, Lot lot, BigDecimal qty, BigDecimal unitCost,
                                          Depot depotSource, Emplacement empSource,
                                          Depot depotDest, Emplacement empDest,
                                          Utilisateur user) {
        MouvementStock mouvement = MouvementStock.builder()
            .numero(generateMouvementNumero())
            .typeMouvement(typeMouvement)
            .referenceDoc(referenceDoc)
            .article(article)
            .lot(lot)
            .qty(qty)
            .unitCost(unitCost)
            .depotSource(depotSource)
            .emplacementSource(empSource)
            .depotDest(depotDest)
            .emplacementDest(empDest)
            .utilisateur(user)
            .createdAt(OffsetDateTime.now())
            .build();
        
        return mouvementStockRepository.save(mouvement);
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
        
        // Création du mouvement avec numéro automatique
        if (typeMouvement.getSens() > 0) {
            creerMouvement(typeMouvement, referenceDoc, article, lot, qty, unitCost,
                          null, null, depot, emplacement, user);
        } else {
            creerMouvement(typeMouvement, referenceDoc, article, lot, qty, unitCost,
                          depot, emplacement, null, null, user);
        }
        
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
    
    // ============ STOCK PAGINATION ============
    
    public Page<Stock> findAllStocks(Pageable pageable) {
        return stockRepository.findAll(pageable);
    }
    
    public List<Stock> findAllStocks() {
        return stockRepository.findAll();
    }
    
    public List<Stock> findAllStocksForTransfer() {
        return stockRepository.findAllStocksWithDepotAndArticle();
    }
    
    public List<Stock> findStocksFiltered(UUID depotId, UUID familleId, String methodeCode, String search) {
        return stockRepository.findStocksFiltered(depotId, familleId, methodeCode, search);
    }

    public List<StockTransfertDTO> findStocksByDepot(UUID depotId) {
        List<Stock> stocks = stockRepository.findByDepotId(depotId);
        return stocks.stream()
            .filter(s -> s.getQtyReel().compareTo(BigDecimal.ZERO) > 0)
            .map(s -> StockTransfertDTO.builder()
                .stockId(s.getId())
                .depotId(s.getDepot().getId())
                .depotName(s.getDepot().getName())
                .articleId(s.getArticle().getId())
                .articleLabel(s.getArticle().getLabel())
                .articleSku(s.getArticle().getSku())
                .qtyReel(s.getQtyReel())
                .qtyReserve(s.getQtyReserve())
                .qtyDisponible(s.getQtyDisponible())
                .lotId(s.getLot() != null ? s.getLot().getId() : null)
                .lotReference(s.getLot() != null ? s.getLot().getNumeroLot() : null)
                .build())
            .collect(Collectors.toList());
    }
    
    // ============ LOTS ============
    
    public List<Lot> findAllLots() {
        return lotRepository.findAll();
    }
    
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
    
    public synchronized String generateReceptionNumero() {
        String prefix = "BR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = bonReceptionRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (bonReceptionRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    public BonReception createReception(BonReception reception, Utilisateur user) {
        reception.setNumero(generateReceptionNumero());
        reception.setStatutCode("BROUILLON");
        reception.setDateReception(OffsetDateTime.now());
        BonReception saved = bonReceptionRepository.save(reception);
        
        auditService.logAction("BON_RECEPTION", saved.getId(), "CREATION", user, null);
        return saved;
    }
    
    public BonReception createReception(BonReceptionDTO dto, Utilisateur user) {
        // Récupérer les entités référencées
        CommandeAchat commande = dto.getCommandeAchatId() != null 
            ? commandeAchatRepository.findById(dto.getCommandeAchatId())
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"))
            : null;
        
        Depot depot = depotRepository.findById(dto.getDepotId())
            .orElseThrow(() -> new RuntimeException("Dépôt non trouvé"));
        
        // Créer le bon de réception
        BonReception reception = BonReception.builder()
            .commandeAchat(commande)
            .depot(depot)
            .site(depot.getSite())
            .dateReception(dto.getDateReception() != null ? dto.getDateReception() : OffsetDateTime.now())
            .statutCode("BROUILLON")
            .build();
        
        // Générer le numéro et sauvegarder d'abord pour obtenir un ID
        reception.setNumero(generateReceptionNumero());
        BonReception savedReception = bonReceptionRepository.save(reception);
        
        // Créer les lignes
        if (dto.getLignes() != null) {
            for (BonReceptionDTO.LigneReceptionDTO ligneDTO : dto.getLignes()) {
                if (ligneDTO.getQtyReceived() == null || ligneDTO.getQtyReceived() <= 0) {
                    continue; // Skip lignes sans quantité
                }
                
                Article article = articleRepository.findById(ligneDTO.getArticleId())
                    .orElseThrow(() -> new RuntimeException("Article non trouvé: " + ligneDTO.getArticleId()));
                
                Emplacement emplacement = ligneDTO.getEmplacementId() != null 
                    ? emplacementRepository.findById(ligneDTO.getEmplacementId()).orElse(null)
                    : null;
                
                // Créer ou récupérer le lot si numéro fourni
                Lot lot = null;
                if (ligneDTO.getNumeroLot() != null && !ligneDTO.getNumeroLot().trim().isEmpty()) {
                    lot = lotRepository.findByNumeroLot(ligneDTO.getNumeroLot())
                        .orElseGet(() -> {
                            Lot newLot = Lot.builder()
                                .numeroLot(ligneDTO.getNumeroLot())
                                .article(article)
                                .statutQualiteCode("QUARANTAINE")
                                .build();
                            
                            if (ligneDTO.getDatePeremption() != null && !ligneDTO.getDatePeremption().trim().isEmpty()) {
                                try {
                                    newLot.setDatePeremption(LocalDate.parse(ligneDTO.getDatePeremption()));
                                } catch (Exception e) {
                                    // Ignore invalid date
                                }
                            }
                            return lotRepository.save(newLot);
                        });
                }
                
                LigneBonReception ligne = LigneBonReception.builder()
                    .bonReception(savedReception)
                    .article(article)
                    .lot(lot)
                    .emplacement(emplacement)
                    .qtyReceived(BigDecimal.valueOf(ligneDTO.getQtyReceived()))
                    .unitCost(ligneDTO.getUnitCost() != null ? BigDecimal.valueOf(ligneDTO.getUnitCost()) : null)
                    .build();
                
                savedReception.addLigne(ligne);
                ligneBonReceptionRepository.save(ligne);
            }
        }
        
        auditService.logAction("BON_RECEPTION", savedReception.getId(), "CREATION", user, null);
        return savedReception;
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
    
    public Optional<MouvementStock> findMouvementById(UUID id) {
        return mouvementStockRepository.findById(id);
    }
    
    public List<MouvementStock> findMouvementsByLot(UUID lotId) {
        return mouvementStockRepository.findByLotIdOrderByCreatedAtAsc(lotId);
    }
    
    // Recherche avec filtres - gestion simplifiée pour éviter les erreurs PostgreSQL
    public Page<MouvementStock> searchMouvements(String typeMouvement, UUID articleId, UUID depotId,
                                                  OffsetDateTime dateDebut, OffsetDateTime dateFin,
                                                  Pageable pageable) {
        // Cas : filtre par type ET article
        if (typeMouvement != null && !typeMouvement.isEmpty() && articleId != null) {
            return mouvementStockRepository.findByTypeMouvementCodeAndArticleIdOrderByCreatedAtDesc(
                    typeMouvement, articleId, pageable);
        }
        // Cas : filtre par type uniquement
        if (typeMouvement != null && !typeMouvement.isEmpty()) {
            return mouvementStockRepository.findByTypeMouvementCodeOrderByCreatedAtDesc(typeMouvement, pageable);
        }
        // Cas : filtre par article uniquement
        if (articleId != null) {
            return mouvementStockRepository.findByArticleIdOrderByCreatedAtDesc(articleId, pageable);
        }
        // Cas : filtre par dépôt uniquement
        if (depotId != null) {
            return mouvementStockRepository.findByDepotIdPaged(depotId, pageable);
        }
        // Cas : filtre par période uniquement
        if (dateDebut != null && dateFin != null) {
            return mouvementStockRepository.findByPeriodPaged(dateDebut, dateFin, pageable);
        }
        // Cas : sans filtre - retourne tout
        return mouvementStockRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    // ============ ALLOCATION FIFO ============
    
    public List<Stock> getAllocationFIFO(UUID articleId, BigDecimal qtyNeeded) {
        List<Stock> availableStocks = stockRepository.findAvailableStockFIFO(articleId);
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
    
    // ============ TRANSFERTS (avec double mouvement) ============
    
    // Transfert entre dépôts - Crée 2 mouvements pour traçabilité complète
    public void transfererStock(Depot depotSource, Emplacement empSource,
                               Depot depotDest, Emplacement empDest,
                               Article article, Lot lot, BigDecimal qty,
                               Utilisateur user) {
        
        // Vérifier disponibilité
        Optional<Stock> stockSourceOpt = stockRepository.findByDepotIdAndEmplacementIdAndArticleIdAndLotId(
            depotSource.getId(), empSource != null ? empSource.getId() : null, 
            article.getId(), lot != null ? lot.getId() : null);
        
        if (stockSourceOpt.isEmpty() || stockSourceOpt.get().getQtyDisponible().compareTo(qty) < 0) {
            throw new RuntimeException("Stock insuffisant pour le transfert");
        }
        
        // Générer une référence unique pour lier les 2 mouvements
        String refTransfert = "TRF-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        
        // Types de mouvement
        TypeMouvement typeSortie = typeMouvementRepository.findById("EXPEDITION")
            .orElseThrow(() -> new RuntimeException("Type mouvement EXPEDITION non trouvé"));
        TypeMouvement typeEntree = typeMouvementRepository.findById("RECEPTION")
            .orElseThrow(() -> new RuntimeException("Type mouvement RECEPTION non trouvé"));
        
        // 1. MOUVEMENT SORTIE (dépôt source)
        Stock source = stockSourceOpt.get();
        source.setQtyReel(source.getQtyReel().subtract(qty));
        stockRepository.save(source);
        
        creerMouvement(typeSortie, refTransfert + "-OUT", article, lot, qty, null,
                      depotSource, empSource, null, null, user);
        
        // 2. MOUVEMENT ENTRÉE (dépôt destination)
        Optional<Stock> stockDestOpt = stockRepository.findByDepotIdAndEmplacementIdAndArticleIdAndLotId(
            depotDest.getId(), empDest != null ? empDest.getId() : null,
            article.getId(), lot != null ? lot.getId() : null);
        
        Stock dest;
        if (stockDestOpt.isPresent()) {
            dest = stockDestOpt.get();
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
        
        creerMouvement(typeEntree, refTransfert + "-IN", article, lot, qty, null,
                      null, null, depotDest, empDest, user);
    }
    
    // Méthode simplifiée de transfert (recherche les entités par ID)
    public void transfererStockSimple(UUID articleId, UUID depotSourceId, UUID depotDestId, 
                                      BigDecimal qty, Utilisateur user) {
        // Trouve le premier stock disponible pour cet article dans le dépôt source
        List<Stock> stocks = stockRepository.findByDepotIdAndArticleId(depotSourceId, articleId);
        if (stocks.isEmpty()) {
            throw new RuntimeException("Aucun stock trouvé pour cet article dans ce dépôt");
        }
        
        Stock stockSource = stocks.get(0);
        if (stockSource.getQtyDisponible().compareTo(qty) < 0) {
            throw new RuntimeException("Stock insuffisant pour le transfert");
        }
        
        Depot depotDest = stockRepository.findByDepotId(depotDestId).stream().findFirst()
            .map(Stock::getDepot)
            .orElseThrow(() -> new RuntimeException("Dépôt destination non trouvé"));
        
        transfererStock(stockSource.getDepot(), stockSource.getEmplacement(),
                       depotDest, null,
                       stockSource.getArticle(), stockSource.getLot(), qty, user);
    }
    
    // ============ CONTRÔLE QUALITÉ ============
    
    public List<ControleQualite> findAllControles() {
        return controleQualiteRepository.findAll();
    }
    
    public void updateLotQualite(UUID lotId, boolean conforme, String notes, Utilisateur user) {
        Lot lot = lotRepository.findById(lotId)
            .orElseThrow(() -> new RuntimeException("Lot non trouvé"));
        
        lot.setStatutQualiteCode(conforme ? "CONFORME" : "REJETE");
        lotRepository.save(lot);
        
        auditService.logAction("LOT", lotId, conforme ? "CONFORME" : "REJET", user, 
            notes);
    }
    
    public Optional<Stock> findStockById(UUID stockId) {
        return stockRepository.findById(stockId);
    }
}
