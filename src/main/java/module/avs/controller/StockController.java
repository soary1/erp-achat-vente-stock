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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {
    
    private final StockService stockService;
    private final ReferentielService referentielService;
    private final UtilisateurService utilisateurService;
    
    private Utilisateur getCurrentUser(Authentication auth) {
        return utilisateurService.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    
    // ============ DÉPÔTS ============
    
    @GetMapping("/depots")
    public String listDepots(Model model) {
        var depots = referentielService.findAllDepots();
        model.addAttribute("depots", depots);
        model.addAttribute("totalDepots", depots.size());
        model.addAttribute("depotsActifs", depots.stream().filter(d -> d.getIsActive() != null && d.getIsActive()).count());
        model.addAttribute("depotsGeoref", depots.stream().filter(d -> d.getLatitude() != null && d.getLongitude() != null).count());
        model.addAttribute("sites", referentielService.findAllSites());
        return "stock/depots";
    }
    
    // ============ CONSULTATION STOCK ============
    
    @GetMapping
    public String listStock(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Stock> stocks = stockService.findAllStocks(pageable);
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
        model.addAttribute("lots", stockService.findExpiredLots());
        model.addAttribute("titre", "Lots périmés");
        return "stock/lots";
    }
    
    @GetMapping("/lots/expirant")
    public String lotsExpirant(@RequestParam(defaultValue = "30") int jours, Model model) {
        model.addAttribute("lots", stockService.findLotsExpiringSoon(jours));
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
        model.addAttribute("commandesEnvoyees", referentielService.findCommandesAchatEnAttente());
        model.addAttribute("depots", referentielService.findAllDepots());
        model.addAttribute("emplacements", referentielService.findAllEmplacements());
        return "stock/reception-form";
    }
    
    @PostMapping("/receptions/save")
    public String saveReception(@ModelAttribute("reception") module.avs.dto.BonReceptionDTO receptionDTO,
                               @RequestParam(required = false) String action,
                               Model model,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            stockService.createReception(receptionDTO, user);
            redirectAttributes.addFlashAttribute("success", "Réception créée avec succès");
            return "redirect:/stock/receptions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("commandesEnvoyees", referentielService.findCommandesAchatEnAttente());
            model.addAttribute("depots", referentielService.findAllDepots());
            model.addAttribute("emplacements", referentielService.findAllEmplacements());
            return "stock/reception-form";
        }
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
                                @RequestParam(required = false) String type,
                                @RequestParam(required = false) UUID articleId,
                                @RequestParam(required = false) UUID depotId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Convertir les dates si présentes
        OffsetDateTime debut = dateDebut != null ? dateDebut.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime fin = dateFin != null ? dateFin.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        
        Page<MouvementStock> mouvements;
        if (type != null || articleId != null || depotId != null || debut != null || fin != null) {
            mouvements = stockService.searchMouvements(type, articleId, depotId, debut, fin, pageable);
        } else {
            mouvements = stockService.findAllMouvements(pageable);
        }
        
        model.addAttribute("mouvements", mouvements);
        model.addAttribute("typesMouvement", referentielService.findAllTypesMouvement());
        model.addAttribute("articles", referentielService.findAllArticles());
        model.addAttribute("depots", referentielService.findAllDepots());
        
        // Conserver les filtres dans le modèle
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedArticleId", articleId);
        model.addAttribute("selectedDepotId", depotId);
        model.addAttribute("selectedDateDebut", dateDebut);
        model.addAttribute("selectedDateFin", dateFin);
        
        return "stock/mouvements";
    }
    
    @GetMapping("/mouvements/{id}")
    public String viewMouvement(@PathVariable UUID id, Model model) {
        stockService.findMouvementById(id).ifPresent(m -> model.addAttribute("mouvement", m));
        return "stock/mouvement-detail";
    }
    
    // ============ TRAÇABILITÉ LOT ============
    
    @GetMapping("/tracabilite/lot/{lotId}")
    public String tracabiliteLot(@PathVariable UUID lotId, Model model) {
        stockService.findLotById(lotId).ifPresent(lot -> {
            model.addAttribute("lot", lot);
            model.addAttribute("mouvements", stockService.findMouvementsByLot(lotId));
        });
        return "stock/tracabilite-lot";
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
            stockService.transfererStockSimple(articleId, depotSourceId, depotDestId, quantite, user);
            redirectAttributes.addFlashAttribute("success", "Transfert effectué avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/mouvements";
    }
    
    // ============ CONTRÔLE QUALITÉ ============
    
    @GetMapping("/controles")
    public String listControles(Model model) {
        model.addAttribute("controles", stockService.findAllControles());
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
            stockService.updateLotQualite(id, conforme, notes, user);
            redirectAttributes.addFlashAttribute("success", "Contrôle qualité enregistré");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/lots";
    }
}
