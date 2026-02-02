package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.article.Article;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Emplacement;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.*;
import module.avs.repository.article.ArticleRepository;
import module.avs.repository.organisation.DepotRepository;
import module.avs.repository.organisation.EmplacementRepository;
import module.avs.repository.stock.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TransfertStockService {
    
    private final TransfertStockRepository transfertRepository;
    private final LigneTransfertStockRepository ligneTransfertRepository;
    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementRepository;
    private final TypeMouvementRepository typeMouvementRepository;
    private final DepotRepository depotRepository;
    private final EmplacementRepository emplacementRepository;
    private final ArticleRepository articleRepository;
    private final LotRepository lotRepository;
    private final AuditService auditService;
    
    // ============ CONSULTATION ============
    
    public List<TransfertStock> findAllTransferts() {
        return transfertRepository.findAll();
    }
    
    public Page<TransfertStock> findAllTransferts(Pageable pageable) {
        return transfertRepository.findAllByOrderByDateDemandeDesc(pageable);
    }
    
    public Optional<TransfertStock> findTransfertById(UUID id) {
        return transfertRepository.findById(id);
    }
    
    public List<TransfertStock> findTransfertsByStatut(String statut) {
        return transfertRepository.findByStatutCode(statut);
    }
    
    public List<TransfertStock> findTransfertsByDepot(UUID depotId) {
        return transfertRepository.findByDepotImplique(depotId);
    }
    
    // ============ NUMÉROTATION ============
    
    public synchronized String generateTransfertNumero() {
        String prefix = "TRF-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = transfertRepository.findMaxNumero(prefix + "%");
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String numero;
        
        do {
            numero = prefix + String.format("%03d", nextNum);
            if (transfertRepository.findByNumero(numero).isPresent()) {
                nextNum++;
            } else {
                break;
            }
        } while (true);
        
        return numero;
    }
    
    // ============ WORKFLOW ============
    
    public TransfertStock createTransfert(UUID depotSourceId, UUID depotDestinationId, String motif, Utilisateur demandeur) {
        Depot depotSource = depotRepository.findById(depotSourceId)
            .orElseThrow(() -> new RuntimeException("Dépôt source non trouvé"));
        Depot depotDest = depotRepository.findById(depotDestinationId)
            .orElseThrow(() -> new RuntimeException("Dépôt destination non trouvé"));
        
        if (depotSourceId.equals(depotDestinationId)) {
            throw new RuntimeException("Le dépôt source et destination doivent être différents");
        }
        
        TransfertStock transfert = TransfertStock.builder()
            .numero(generateTransfertNumero())
            .depotSource(depotSource)
            .depotDest(depotDest)
            .motif(motif)
            .statutCode("DEMANDE")
            .demandeur(demandeur)
            .dateDemande(OffsetDateTime.now())
            .build();
        
        TransfertStock saved = transfertRepository.save(transfert);
        auditService.logAction("TRANSFERT_STOCK", saved.getId(), "CREATION", demandeur, null);
        return saved;
    }
    
    public TransfertStock createAndExecuteTransfert(UUID depotSourceId, UUID depotDestinationId, 
                                                     String motif, List<UUID> articleIds, 
                                                     List<BigDecimal> quantites, List<UUID> lotIds,
                                                     List<UUID> emplacementIds, Utilisateur utilisateur) {
        // Validation
        if (articleIds == null || articleIds.isEmpty()) {
            throw new RuntimeException("Veuillez ajouter au moins un article à transférer");
        }
        
        Depot depotSource = depotRepository.findById(depotSourceId)
            .orElseThrow(() -> new RuntimeException("Dépôt source non trouvé"));
        Depot depotDest = depotRepository.findById(depotDestinationId)
            .orElseThrow(() -> new RuntimeException("Dépôt destination non trouvé"));
        
        if (depotSourceId.equals(depotDestinationId)) {
            throw new RuntimeException("Le dépôt source et destination doivent être différents");
        }
        
        // Créer le transfert
        TransfertStock transfert = TransfertStock.builder()
            .numero(generateTransfertNumero())
            .depotSource(depotSource)
            .depotDest(depotDest)
            .motif(motif)
            .statutCode("COMPLETE")
            .demandeur(utilisateur)
            .expediteur(utilisateur)
            .recepteur(utilisateur)
            .dateDemande(OffsetDateTime.now())
            .dateExpedition(OffsetDateTime.now())
            .dateReception(OffsetDateTime.now())
            .build();
        
        TransfertStock saved = transfertRepository.save(transfert);
        
        // Ajouter les lignes et effectuer les mouvements de stock
        TypeMouvement typeMouvementSortie = typeMouvementRepository.findById("TRANSFERT_SORTIE")
            .orElse(typeMouvementRepository.findById("SORTIE").orElse(null));
        TypeMouvement typeMouvementEntree = typeMouvementRepository.findById("TRANSFERT_ENTREE")
            .orElse(typeMouvementRepository.findById("ENTREE").orElse(null));
        
        for (int i = 0; i < articleIds.size(); i++) {
            UUID articleId = articleIds.get(i);
            BigDecimal quantite = quantites.get(i);
            UUID lotId = (lotIds != null && i < lotIds.size() && lotIds.get(i) != null) ? lotIds.get(i) : null;
            UUID emplacementId = (emplacementIds != null && i < emplacementIds.size() && emplacementIds.get(i) != null) ? emplacementIds.get(i) : null;
            
            Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));
            
            Lot lot = null;
            if (lotId != null) {
                lot = lotRepository.findById(lotId).orElse(null);
            }
            
            Emplacement emplacementDest = null;
            if (emplacementId != null) {
                emplacementDest = emplacementRepository.findById(emplacementId).orElse(null);
            }
            
            // Créer la ligne de transfert
            LigneTransfertStock ligne = LigneTransfertStock.builder()
                .transfert(saved)
                .article(article)
                .lot(lot)
                .emplacementDest(emplacementDest)
                .qtyDemandee(quantite)
                .qtyExpedie(quantite)
                .qtyRecue(quantite)
                .build();
            
            ligneTransfertRepository.save(ligne);
            
            // Décrémenter le stock source
            Stock stockSource = stockRepository.findByDepotIdAndArticleId(
                depotSourceId, articleId
            ).stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Stock source non trouvé pour " + article.getLabel()));
            
            if (stockSource.getQtyDisponible().compareTo(quantite) < 0) {
                throw new RuntimeException("Stock insuffisant pour " + article.getLabel() + 
                    ". Disponible: " + stockSource.getQtyDisponible() + ", Demandé: " + quantite);
            }
            
            stockSource.setQtyReel(stockSource.getQtyReel().subtract(quantite));
            stockRepository.save(stockSource);
            
            // Créer mouvement de sortie
            if (typeMouvementSortie != null) {
                MouvementStock mouvementSortie = MouvementStock.builder()
                    .numero("MVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .typeMouvement(typeMouvementSortie)
                    .depotSource(depotSource)
                    .article(article)
                    .lot(lot)
                    .qty(quantite.negate())
                    .referenceDoc("Transfert " + saved.getNumero())
                    .createdAt(OffsetDateTime.now())
                    .build();
                mouvementRepository.save(mouvementSortie);
            }
            
            // Incrémenter ou créer le stock destination
            Stock stockDest = stockRepository.findByDepotIdAndArticleId(
                depotDestinationId, articleId
            ).stream().findFirst().orElse(null);
            
            if (stockDest == null) {
                stockDest = Stock.builder()
                    .depot(depotDest)
                    .emplacement(emplacementDest)
                    .article(article)
                    .lot(lot)
                    .qtyReel(quantite)
                    .qtyReserve(BigDecimal.ZERO)
                    .build();
            } else {
                // Si un emplacement est spécifié et différent, on met à jour
                if (emplacementDest != null) {
                    stockDest.setEmplacement(emplacementDest);
                }
                stockDest.setQtyReel(stockDest.getQtyReel().add(quantite));
            }
            stockRepository.save(stockDest);
            
            // Créer mouvement d'entrée
            if (typeMouvementEntree != null) {
                MouvementStock mouvementEntree = MouvementStock.builder()
                    .numero("MVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .typeMouvement(typeMouvementEntree)
                    .depotDest(depotDest)
                    .article(article)
                    .lot(lot)
                    .qty(quantite)
                    .referenceDoc("Transfert " + saved.getNumero())
                    .createdAt(OffsetDateTime.now())
                    .build();
                mouvementRepository.save(mouvementEntree);
            }
        }
        
        auditService.logAction("TRANSFERT_STOCK", saved.getId(), "TRANSFERT_IMMEDIAT", utilisateur, 
            "Transfert de " + depotSource.getName() + " vers " + depotDest.getName());
        
        return transfertRepository.findById(saved.getId())
            .orElseThrow(() -> new RuntimeException("Transfert non trouvé"));
    }
    
    public TransfertStock createTransfert(TransfertStock transfert, Utilisateur demandeur) {
        transfert.setNumero(generateTransfertNumero());
        transfert.setStatutCode("DEMANDE");
        transfert.setDemandeur(demandeur);
        transfert.setDateDemande(OffsetDateTime.now());
        
        TransfertStock saved = transfertRepository.save(transfert);
        auditService.logAction("TRANSFERT_STOCK", saved.getId(), "CREATION", demandeur, null);
        return saved;
    }
    
    public TransfertStock approuverTransfert(UUID transfertId, Utilisateur approbateur) {
        TransfertStock transfert = transfertRepository.findById(transfertId)
            .orElseThrow(() -> new RuntimeException("Transfert non trouvé"));
        
        if (!"DEMANDE".equals(transfert.getStatutCode())) {
            throw new RuntimeException("Seuls les transferts en statut DEMANDE peuvent être approuvés");
        }
        
        transfert.setStatutCode("APPROUVE");
        transfert.setApprobateur(approbateur);
        transfert.setDateApprobation(OffsetDateTime.now());
        
        TransfertStock saved = transfertRepository.save(transfert);
        auditService.logWorkflow("TRANSFERT_STOCK", transfertId, "DEMANDE", "APPROUVE", approbateur, "APPROBATION", null);
        return saved;
    }
    
    public TransfertStock expedierTransfert(UUID transfertId, Utilisateur expediteur) {
        TransfertStock transfert = transfertRepository.findById(transfertId)
            .orElseThrow(() -> new RuntimeException("Transfert non trouvé"));
        
        if (!"APPROUVE".equals(transfert.getStatutCode())) {
            throw new RuntimeException("Seuls les transferts approuvés peuvent être expédiés");
        }
        
        TypeMouvement typeMouvementSortie = typeMouvementRepository.findById("TRANSFERT_SORTIE")
            .orElseThrow(() -> new RuntimeException("Type mouvement TRANSFERT_SORTIE non trouvé"));
        
        // Créer les mouvements de sortie et décrémenter le stock source
        for (LigneTransfertStock ligne : transfert.getLignes()) {
            // Vérifier stock disponible
            Stock stock = stockRepository.findByDepotIdAndArticleIdAndLotId(
                transfert.getDepotSource().getId(),
                ligne.getArticle().getId(),
                ligne.getLot() != null ? ligne.getLot().getId() : null
            ).orElseThrow(() -> new RuntimeException("Stock insuffisant pour " + ligne.getArticle().getLabel()));
            
            if (stock.getQtyDisponible().compareTo(ligne.getQtyDemandee()) < 0) {
                throw new RuntimeException("Stock insuffisant pour " + ligne.getArticle().getLabel());
            }
            
            // Décrémenter stock source
            stock.setQtyReel(stock.getQtyReel().subtract(ligne.getQtyDemandee()));
            stockRepository.save(stock);
            
            // Créer mouvement de sortie
            creerMouvement(typeMouvementSortie, transfert, ligne, expediteur, true);
            
            // Marquer comme expédié
            ligne.setQtyExpedie(ligne.getQtyDemandee());
            ligneTransfertRepository.save(ligne);
        }
        
        transfert.setStatutCode("EN_TRANSIT");
        transfert.setExpediteur(expediteur);
        transfert.setDateExpedition(OffsetDateTime.now());
        
        TransfertStock saved = transfertRepository.save(transfert);
        auditService.logWorkflow("TRANSFERT_STOCK", transfertId, "APPROUVE", "EN_TRANSIT", expediteur, "EXPEDITION", null);
        return saved;
    }
    
    public TransfertStock recevoirTransfert(UUID transfertId, Utilisateur recepteur) {
        TransfertStock transfert = transfertRepository.findById(transfertId)
            .orElseThrow(() -> new RuntimeException("Transfert non trouvé"));
        
        if (!"EN_TRANSIT".equals(transfert.getStatutCode()) && !"EXPEDIE".equals(transfert.getStatutCode())) {
            throw new RuntimeException("Seuls les transferts en transit peuvent être réceptionnés");
        }
        
        TypeMouvement typeMouvementEntree = typeMouvementRepository.findById("TRANSFERT_ENTREE")
            .orElseThrow(() -> new RuntimeException("Type mouvement TRANSFERT_ENTREE non trouvé"));
        
        // Créer les mouvements d'entrée et incrémenter le stock destination
        for (LigneTransfertStock ligne : transfert.getLignes()) {
            // Trouver ou créer le stock destination
            Stock stockDest = stockRepository.findByDepotIdAndArticleIdAndLotId(
                transfert.getDepotDest().getId(),
                ligne.getArticle().getId(),
                ligne.getLot() != null ? ligne.getLot().getId() : null
            ).orElseGet(() -> {
                Stock newStock = Stock.builder()
                    .depot(transfert.getDepotDest())
                    .emplacement(ligne.getEmplacementDest())
                    .article(ligne.getArticle())
                    .lot(ligne.getLot())
                    .qtyReel(BigDecimal.ZERO)
                    .qtyReserve(BigDecimal.ZERO)
                    .build();
                return stockRepository.save(newStock);
            });
            
            // Incrémenter stock destination
            stockDest.setQtyReel(stockDest.getQtyReel().add(ligne.getQtyExpedie()));
            stockRepository.save(stockDest);
            
            // Créer mouvement d'entrée
            creerMouvement(typeMouvementEntree, transfert, ligne, recepteur, false);
            
            // Marquer comme reçu
            ligne.setQtyRecue(ligne.getQtyExpedie());
            ligneTransfertRepository.save(ligne);
        }
        
        transfert.setStatutCode("CLOTURE");
        transfert.setRecepteur(recepteur);
        transfert.setDateReception(OffsetDateTime.now());
        
        TransfertStock saved = transfertRepository.save(transfert);
        auditService.logWorkflow("TRANSFERT_STOCK", transfertId, "EN_TRANSIT", "CLOTURE", recepteur, "RECEPTION", null);
        return saved;
    }
    
    public TransfertStock annulerTransfert(UUID transfertId, Utilisateur annuleur, String motif) {
        TransfertStock transfert = transfertRepository.findById(transfertId)
            .orElseThrow(() -> new RuntimeException("Transfert non trouvé"));
        
        if ("CLOTURE".equals(transfert.getStatutCode())) {
            throw new RuntimeException("Un transfert clôturé ne peut pas être annulé");
        }
        
        transfert.setStatutCode("ANNULE");
        transfert.setNotes(motif);
        
        TransfertStock saved = transfertRepository.save(transfert);
        auditService.logWorkflow("TRANSFERT_STOCK", transfertId, transfert.getStatutCode(), "ANNULE", annuleur, "ANNULATION", motif);
        return saved;
    }
    
    // ============ TRANSFERT D'EMPLACEMENT (même dépôt) ============
    
    public void transfererEmplacement(UUID stockId, UUID nouvelEmplacementId, Utilisateur user) {
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> new RuntimeException("Stock non trouvé"));
        
        Emplacement nouvelEmplacement = emplacementRepository.findById(nouvelEmplacementId)
            .orElseThrow(() -> new RuntimeException("Emplacement non trouvé"));
        
        if (!stock.getDepot().getId().equals(nouvelEmplacement.getDepot().getId())) {
            throw new RuntimeException("L'emplacement doit être dans le même dépôt");
        }
        
        Emplacement ancienEmplacement = stock.getEmplacement();
        BigDecimal qty = stock.getQtyReel();
        
        // Créer mouvement de transfert d'emplacement
        TypeMouvement typeMouvement = typeMouvementRepository.findById("TRANSFERT_EMPLACEMENT")
            .orElseThrow(() -> new RuntimeException("Type mouvement TRANSFERT_EMPLACEMENT non trouvé"));
        
        String numeroMvt = "MVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = mouvementRepository.findMaxNumero(numeroMvt);
        numeroMvt += String.format("%05d", (maxNum != null ? maxNum : 0) + 1);
        
        MouvementStock mouvement = MouvementStock.builder()
            .numero(numeroMvt)
            .typeMouvement(typeMouvement)
            .referenceDoc("TRANSFERT_EMPLACEMENT")
            .article(stock.getArticle())
            .lot(stock.getLot())
            .depotSource(stock.getDepot())
            .emplacementSource(ancienEmplacement)
            .depotDest(stock.getDepot())
            .emplacementDest(nouvelEmplacement)
            .qty(qty)
            .utilisateur(user)
            .createdAt(OffsetDateTime.now())
            .build();
        
        mouvementRepository.save(mouvement);
        
        // Mettre à jour l'emplacement du stock
        stock.setEmplacement(nouvelEmplacement);
        stockRepository.save(stock);
        
        auditService.logAction("STOCK", stock.getId(), "TRANSFERT_EMPLACEMENT", user, 
            "De " + (ancienEmplacement != null ? ancienEmplacement.getCode() : "N/A") + " vers " + nouvelEmplacement.getCode());
    }
    
    // ============ HELPER METHODS ============
    
    private void creerMouvement(TypeMouvement typeMouvement, TransfertStock transfert,
                                LigneTransfertStock ligne, Utilisateur user, boolean isSortie) {
        String numeroMvt = "MVT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = mouvementRepository.findMaxNumero(numeroMvt);
        numeroMvt += String.format("%05d", (maxNum != null ? maxNum : 0) + 1);
        
        MouvementStock.MouvementStockBuilder builder = MouvementStock.builder()
            .numero(numeroMvt)
            .typeMouvement(typeMouvement)
            .referenceDoc(transfert.getNumero())
            .article(ligne.getArticle())
            .lot(ligne.getLot())
            .qty(ligne.getQtyDemandee())
            .unitCost(ligne.getUnitCost())
            .utilisateur(user)
            .createdAt(OffsetDateTime.now());
        
        if (isSortie) {
            builder.depotSource(transfert.getDepotSource())
                   .emplacementSource(ligne.getEmplacementSource());
        } else {
            builder.depotDest(transfert.getDepotDest())
                   .emplacementDest(ligne.getEmplacementDest());
        }
        
        mouvementRepository.save(builder.build());
    }
}
