package module.avs.service;

import lombok.RequiredArgsConstructor;
import module.avs.model.article.Article;
import module.avs.model.article.FamilleArticle;
import module.avs.model.stock.Stock;
import module.avs.repository.article.ArticleRepository;
import module.avs.repository.article.FamilleArticleRepository;
import module.avs.repository.stock.StockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    private final FamilleArticleRepository familleArticleRepository;
    private final StockRepository stockRepository;
    
    // ============ GESTION DES ARTICLES ============
    
    public List<Article> findAllArticles() {
        return articleRepository.findAll();
    }
    
    public List<Article> findActiveArticles() {
        return articleRepository.findByIsActiveTrue();
    }
    
    public Article findById(UUID id) {
        return articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article non trouvé"));
    }
    
    public Article findBySku(String sku) {
        return articleRepository.findBySku(sku)
            .orElseThrow(() -> new RuntimeException("Article non trouvé"));
    }
    
    public Page<Article> searchArticles(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return articleRepository.searchArticles(search, pageable);
        }
        return articleRepository.findAll(pageable);
    }
    
    public List<Article> findByFamille(UUID familleId) {
        return articleRepository.findByFamilleIdAndIsActiveTrue(familleId);
    }
    
    public Article save(Article article) {
        // Vérifier l'unicité du SKU
        if (article.getId() == null && articleRepository.findBySku(article.getSku()).isPresent()) {
            throw new RuntimeException("Un article avec ce SKU existe déjà");
        }
        return articleRepository.save(article);
    }
    
    public void delete(UUID id) {
        Article article = findById(id);
        article.setIsActive(false);
        articleRepository.save(article);
    }
    
    // ============ GESTION DES FAMILLES ============
    
    public List<FamilleArticle> findAllFamilles() {
        return familleArticleRepository.findAll();
    }
    
    public FamilleArticle findFamilleById(UUID id) {
        return familleArticleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Famille non trouvée"));
    }
    
    // ============ INFORMATIONS DE STOCK ============
    
    public List<Stock> getStocksByArticle(UUID articleId) {
        return stockRepository.findByArticleId(articleId);
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
}
