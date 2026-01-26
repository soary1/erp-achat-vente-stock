package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.*;
import module.avs.service.ReferentielService;
import module.avs.service.StockService;
import module.avs.service.UtilisateurService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {
    
    private final StockService stockService;
    private final ReferentielService referentielService;
    private final UtilisateurService utilisateurService;
    
    private Utilisateur getCurrentUser(Authentication auth) {
        return utilisateurService.findByLogin(auth.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    
    // ============ CONSULTATION STOCK ============
    
    @GetMapping
    public String listStock(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Stock> stocks = stockService.findAllStock(pageable);
        model.addAttribute("stocks", stocks);
        return "stock/stocks";
    }
    
    @GetMapping("/depot/{depotId}")
    public String stockByDepot(@PathVariable UUID depotId, Model model) {
        model.addAttribute("stocks", stockService.findStockByDepot(depotId));
        return "stock/stocks";
    }
    
    @GetMapping("/article/{articleId}")
    public String stockByArticle(@PathVariable UUID articleId, Model model) {
        model.addAttribute("stocks", stockService.findStockByArticle(articleId));
        return "stock/article-stock";
    }
    
    // ============ LOTS ============
    
    @GetMapping("/lots")
    public String listLots(Model model) {
        model.addAttribute("lots", stockService.findAllLots());
        return "stock/lots";
    }
    
    @GetMapping("/lots/perimes")
    public String lotsPerimes(Model model) {
        model.addAttribute("lots", stockService.getLotsPerimes());
        model.addAttribute("titre", "Lots périmés");
        return "stock/lots";
    }
    
    @GetMapping("/lots/expirant")
    public String lotsExpirant(@RequestParam(defaultValue = "30") int jours, Model model) {
        model.addAttribute("lots", stockService.getLotsExpirantBientot(jours));
        model.addAttribute("titre", "Lots expirant dans " + jours + " jours");
        return "stock/lots";
    }
    
    // ============ RÉCEPTIONS ============
    
    @GetMapping("/receptions")
    public String listReceptions(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BonReception> receptions = stockService.findAllReceptions(pageable);
        model.addAttribute("receptions", receptions);
        return "stock/receptions";
    }
    
    @GetMapping("/receptions/{id}")
    public String viewReception(@PathVariable UUID id, Model model) {
        stockService.findReceptionById(id).ifPresent(r -> model.addAttribute("reception", r));
        return "stock/reception-detail";
    }
    
    @GetMapping("/receptions/add")
    public String addReceptionForm(Model model) {
        model.addAttribute("reception", new BonReception());
        model.addAttribute("commandesAchat", referentielService.findCommandesAchatEnAttente());
        model.addAttribute("depots", referentielService.findAllDepots());
        return "stock/reception-form";
    }
    
    @PostMapping("/receptions/save")
    public String saveReception(@Valid @ModelAttribute BonReception reception,
                               BindingResult result,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "stock/reception-form";
        }
        Utilisateur user = getCurrentUser(auth);
        stockService.createReception(reception, user);
        redirectAttributes.addFlashAttribute("success", "Réception créée avec succès");
        return "redirect:/stock/receptions";
    }
    
    @PostMapping("/receptions/{id}/validate")
    public String validerReception(@PathVariable UUID id,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            stockService.validerReception(id, user);
            redirectAttributes.addFlashAttribute("success", "Réception validée - Stock mis à jour");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/receptions/" + id;
    }
    
    // ============ MOUVEMENTS ============
    
    @GetMapping("/mouvements")
    public String listMouvements(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        model.addAttribute("mouvements", stockService.findAllMouvements(pageable));
        return "stock/mouvements";
    }
    
    // ============ TRANSFERTS ============
    
    @GetMapping("/transferts/add")
    public String addTransfertForm(Model model) {
        model.addAttribute("depots", referentielService.findAllDepots());
        model.addAttribute("articles", referentielService.findAllArticles());
        return "stock/transfert-form";
    }
    
    @PostMapping("/transferts/execute")
    public String executerTransfert(@RequestParam UUID articleId,
                                    @RequestParam UUID depotSourceId,
                                    @RequestParam UUID depotDestId,
                                    @RequestParam BigDecimal quantite,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            stockService.transfererStock(articleId, depotSourceId, depotDestId, quantite, user);
            redirectAttributes.addFlashAttribute("success", "Transfert effectué avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/mouvements";
    }
    
    // ============ CONTRÔLE QUALITÉ ============
    
    @GetMapping("/controles")
    public String listControles(Model model) {
        model.addAttribute("controles", stockService.findAllControlesQualite());
        return "stock/controles";
    }
    
    @PostMapping("/lots/{id}/controle")
    public String enregistrerControle(@PathVariable UUID id,
                                      @RequestParam String resultat,
                                      @RequestParam(required = false) String notes,
                                      Authentication auth,
                                      RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            boolean conforme = "CONFORME".equals(resultat);
            stockService.enregistrerControleQualite(id, user, conforme, notes);
            redirectAttributes.addFlashAttribute("success", "Contrôle qualité enregistré");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/lots";
    }
}
