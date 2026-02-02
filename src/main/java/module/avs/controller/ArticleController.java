package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.article.Article;
import module.avs.model.article.FamilleArticle;
import module.avs.model.referentiel.TypeTaxe;
import module.avs.model.referentiel.UniteMesure;
import module.avs.model.stock.Stock;
import module.avs.repository.article.FamilleArticleRepository;
import module.avs.repository.referentiel.TypeTaxeRepository;
import module.avs.repository.referentiel.UniteMesureRepository;
import module.avs.service.ArticleService;
import module.avs.service.UtilisateurService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final FamilleArticleRepository familleArticleRepository;
    private final UniteMesureRepository uniteMesureRepository;
    private final TypeTaxeRepository typeTaxeRepository;
    private final UtilisateurService utilisateurService;

    @GetMapping
    public String listArticles(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID famille,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("sku").ascending());
        Page<Article> articles;
        
        if (search != null && !search.isEmpty()) {
            articles = articleService.searchArticles(search, pageable);
            model.addAttribute("search", search);
        } else {
            articles = articleService.searchArticles("", pageable);
        }
        
        model.addAttribute("articles", articles.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("totalItems", articles.getTotalElements());
        model.addAttribute("familles", familleArticleRepository.findAll());
        model.addAttribute("familleId", famille);
        model.addAttribute("actif", actif);
        
        return "articles/articles";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("article", new Article());
        model.addAttribute("familles", familleArticleRepository.findAll());
        model.addAttribute("unites", uniteMesureRepository.findAll());
        model.addAttribute("taxes", typeTaxeRepository.findAll());
        model.addAttribute("societe", utilisateurService.getCurrentUserSociete());
        return "articles/article-form";
    }

    @PostMapping("/add")
    public String addArticle(@ModelAttribute Article article, RedirectAttributes redirectAttributes) {
        try {
            // Récupérer la société de l'utilisateur
            article.setSociete(utilisateurService.getCurrentUserSociete());
            
            Article saved = articleService.save(article);
            redirectAttributes.addFlashAttribute("success", "Article créé avec succès");
            return "redirect:/articles/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création : " + e.getMessage());
            return "redirect:/articles/add";
        }
    }

    @GetMapping("/{id}")
    public String viewArticle(@PathVariable UUID id, Model model) {
        Article article = articleService.findById(id);
        List<Stock> stocks = articleService.getStocksByArticle(id);
        BigDecimal stockTotal = articleService.getStockTotal(id);
        BigDecimal stockDisponible = articleService.getStockDisponible(id);
        
        model.addAttribute("article", article);
        model.addAttribute("stocks", stocks);
        model.addAttribute("stockTotal", stockTotal);
        model.addAttribute("stockDisponible", stockDisponible);
        
        return "articles/article-detail";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        Article article = articleService.findById(id);
        model.addAttribute("article", article);
        model.addAttribute("familles", familleArticleRepository.findAll());
        model.addAttribute("unites", uniteMesureRepository.findAll());
        model.addAttribute("taxes", typeTaxeRepository.findAll());
        return "articles/article-form";
    }

    @PostMapping("/edit/{id}")
    public String editArticle(@PathVariable UUID id, @ModelAttribute Article article, RedirectAttributes redirectAttributes) {
        try {
            article.setId(id);
            // Conserver la société existante
            Article existing = articleService.findById(id);
            article.setSociete(existing.getSociete());
            
            articleService.save(article);
            redirectAttributes.addFlashAttribute("success", "Article modifié avec succès");
            return "redirect:/articles/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la modification : " + e.getMessage());
            return "redirect:/articles/edit/" + id;
        }
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            Article article = articleService.findById(id);
            article.setIsActive(!article.getIsActive());
            articleService.save(article);
            redirectAttributes.addFlashAttribute("success", 
                article.getIsActive() ? "Article activé" : "Article désactivé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/articles/" + id;
    }
}
