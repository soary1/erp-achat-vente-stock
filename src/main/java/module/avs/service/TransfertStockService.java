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
    
    public String generateTransfertNumero() {
        String prefix = "TRF-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        Integer maxNum = transfertRepository.findMaxNumero(prefix + "%");
        return prefix + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);
    }
    
    // ============ WORKFLOW ============
    
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
