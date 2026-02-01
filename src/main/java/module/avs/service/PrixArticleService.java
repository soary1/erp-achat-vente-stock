package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.article.Article;
import module.avs.model.organisation.Depot;
import module.avs.model.stock.Lot;
import module.avs.model.stock.MouvementStock;
import module.avs.model.stock.Stock;
import module.avs.repository.article.ArticleRepository;
import module.avs.repository.stock.MouvementStockRepository;
import module.avs.repository.stock.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Service pour calculer le prix de vente d'un article selon la méthode de valorisation
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrixArticleService {
    
    private final ArticleRepository articleRepository;
    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementStockRepository;
    
    /**
     * Calcule le prix de vente d'un article selon la méthode de valorisation de sa famille
     * 
     * @param articleId ID de l'article
     * @param depotId ID du dépôt (optionnel)
     * @param siteId ID du site (optionnel) - filtre les dépôts du site
     * @param margePercent Marge en pourcentage à appliquer sur le coût (ex: 20 pour 20%)
     * @return Prix de vente calculé
     */
    public BigDecimal calculerPrixVente(UUID articleId, UUID depotId, UUID siteId, BigDecimal margePercent) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article non trouvé"));
        
        String methodeValorisation = article.getFamille().getMethodeValorisation().getCode();
        
        BigDecimal coutUnitaire = switch (methodeValorisation) {
            case "FIFO" -> calculerPrixFIFO(articleId, depotId, siteId);
            case "LIFO" -> calculerPrixLIFO(articleId, depotId, siteId);
            case "CUMP" -> calculerPrixCUMP(articleId, depotId, siteId);
            default -> throw new RuntimeException("Méthode de valorisation non supportée: " + methodeValorisation);
        };
        
        // Appliquer la marge
        if (margePercent != null && margePercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal marge = coutUnitaire.multiply(margePercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            return coutUnitaire.add(marge);
        }
        
        return coutUnitaire;
    }
    
    /**
     * Calcule le prix selon FIFO (Premier Entré, Premier Sorti)
     * Utilise le coût du lot le plus ancien disponible
     */
    private BigDecimal calculerPrixFIFO(UUID articleId, UUID depotId, UUID siteId) {
        List<Stock> stocks;
        
        if (depotId != null) {
            stocks = stockRepository.findByDepotIdAndArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationAsc(
                    depotId, articleId, BigDecimal.ZERO);
        } else if (siteId != null) {
            // Filtrer par site : récupérer tous les stocks et filtrer ceux du site
            stocks = stockRepository.findByArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationAsc(
                    articleId, BigDecimal.ZERO);
            stocks = stocks.stream()
                .filter(s -> s.getDepot().getSite() != null && s.getDepot().getSite().getId().equals(siteId))
                .toList();
        } else {
            stocks = stockRepository.findByArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationAsc(
                    articleId, BigDecimal.ZERO);
        }
        
        if (stocks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Récupérer le coût du lot le plus ancien (FIFO)
        Stock stockFifo = stocks.get(0);
        return getCoutUnitaireLot(stockFifo);
    }
    
    /**
     * Calcule le prix selon LIFO (Dernier Entré, Premier Sorti)
     * Utilise le coût du lot le plus récent disponible
     */
    private BigDecimal calculerPrixLIFO(UUID articleId, UUID depotId, UUID siteId) {
        List<Stock> stocks;
        
        if (depotId != null) {
            stocks = stockRepository.findByDepotIdAndArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationDesc(
                depotId, articleId, BigDecimal.ZERO);
        } else if (siteId != null) {
            // Filtrer par site : récupérer tous les stocks et filtrer ceux du site
            stocks = stockRepository.findByArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationDesc(
                articleId, BigDecimal.ZERO);
            stocks = stocks.stream()
                .filter(s -> s.getDepot().getSite() != null && s.getDepot().getSite().getId().equals(siteId))
                .toList();
        } else {
            stocks = stockRepository.findByArticleIdAndQtyReelGreaterThanOrderByLotDateFabricationDesc(
                articleId, BigDecimal.ZERO);
        }
        
        if (stocks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Récupérer le coût du lot le plus récent (LIFO)
        Stock stockLifo = stocks.get(0);
        return getCoutUnitaireLot(stockLifo);
    }
    
    /**
     * Calcule le prix selon CUMP (Coût Unitaire Moyen Pondéré)
     * Fait la moyenne pondérée de tous les lots en stock
     */
    private BigDecimal calculerPrixCUMP(UUID articleId, UUID depotId, UUID siteId) {
        List<Stock> stocks;
        
        if (depotId != null) {
            stocks = stockRepository.findByDepotIdAndArticleIdAndQtyReelGreaterThan(
                depotId, articleId, BigDecimal.ZERO);
        } else if (siteId != null) {
            // Filtrer par site : récupérer tous les stocks et filtrer ceux du site
            stocks = stockRepository.findByArticleIdAndQtyReelGreaterThan(
                articleId, BigDecimal.ZERO);
            stocks = stocks.stream()
                .filter(s -> s.getDepot().getSite() != null && s.getDepot().getSite().getId().equals(siteId))
                .toList();
        } else {
            stocks = stockRepository.findByArticleIdAndQtyReelGreaterThan(
                articleId, BigDecimal.ZERO);
        }
        
        if (stocks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal valeurTotale = BigDecimal.ZERO;
        BigDecimal quantiteTotale = BigDecimal.ZERO;
        
        for (Stock stock : stocks) {
            BigDecimal coutUnitaire = getCoutUnitaireLot(stock);
            BigDecimal valeurStock = coutUnitaire.multiply(stock.getQtyReel());
            
            valeurTotale = valeurTotale.add(valeurStock);
            quantiteTotale = quantiteTotale.add(stock.getQtyReel());
        }
        
        if (quantiteTotale.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return valeurTotale.divide(quantiteTotale, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Récupère le coût unitaire d'un lot depuis les mouvements de stock
     * Si pas de mouvement, retourne 0
     */
    private BigDecimal getCoutUnitaireLot(Stock stock) {
        if (stock.getLot() == null) {
            // Pas de lot, chercher le dernier mouvement d'entrée pour cet article dans ce dépôt
            List<MouvementStock> mouvements = mouvementStockRepository
                .findByArticleIdAndDepotDestIdAndUnitCostIsNotNullOrderByCreatedAtDesc(
                    stock.getArticle().getId(), 
                    stock.getDepot().getId()
                );
            
            if (!mouvements.isEmpty()) {
                return mouvements.get(0).getUnitCost();
            }
        } else {
            // Avec lot, chercher le mouvement d'entrée de ce lot spécifique
            List<MouvementStock> mouvements = mouvementStockRepository
                .findByArticleIdAndLotIdAndUnitCostIsNotNullOrderByCreatedAtDesc(
                    stock.getArticle().getId(),
                    stock.getLot().getId()
                );
            
            if (!mouvements.isEmpty()) {
                return mouvements.get(0).getUnitCost();
            }
        }
        
        // Si aucun mouvement trouvé, retourner 0
        return BigDecimal.ZERO;
    }
    
    /**
     * Récupère le prix de vente depuis la liste tarifaire si elle existe
     */
    public BigDecimal getPrixTarifaire(UUID articleId, UUID listeId) {
        // TODO: Implémenter la recherche dans ligne_liste_tarifaire
        return null;
    }
}
