package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.Inventaire;
import module.avs.model.stock.LigneInventaire;
import module.avs.model.stock.SaisieInventaire;
import module.avs.service.InventaireService;
import module.avs.service.ReferentielService;
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
@RequestMapping("/inventaires")
@RequiredArgsConstructor
public class InventaireController {
    
    private final InventaireService inventaireService;
    private final ReferentielService referentielService;
    private final UtilisateurService utilisateurService;
    
    private Utilisateur getCurrentUser(Authentication auth) {
        return utilisateurService.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    
    // ============ LISTE ET DÉTAILS ============
    
    @GetMapping
    public String listInventaires(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Inventaire> inventaires = inventaireService.findAllInventaires(pageable);
        model.addAttribute("inventaires", inventaires);
        return "inventaires/inventaires";
    }
    
    @GetMapping("/{id}")
    public String viewInventaire(@PathVariable UUID id, Model model) {
        inventaireService.findInventaireById(id).ifPresent(inv -> {
            model.addAttribute("inventaire", inv);
            model.addAttribute("stats", inventaireService.getInventaireStats(id));
        });
        return "inventaires/inventaire-detail";
    }
    
    @GetMapping("/en-cours")
    public String inventairesEnCours(Model model) {
        model.addAttribute("inventaires", inventaireService.findInventairesByStatut("EN_COURS"));
        return "inventaires/inventaires";
    }
    
    // ============ CRÉATION ============
    
    @GetMapping("/add")
    public String addInventaireForm(Model model) {
        model.addAttribute("inventaire", new Inventaire());
        model.addAttribute("depots", referentielService.findAllDepots());
        model.addAttribute("emplacements", referentielService.findAllEmplacements());
        model.addAttribute("familles", referentielService.findAllFamilles());
        return "inventaires/inventaire-form";
    }
    
    @PostMapping("/save")
    public String saveInventaire(@Valid @ModelAttribute Inventaire inventaire,
                                 BindingResult result,
                                 Authentication auth,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("depots", referentielService.findAllDepots());
            model.addAttribute("emplacements", referentielService.findAllEmplacements());
            model.addAttribute("familles", referentielService.findAllFamilles());
            return "inventaires/inventaire-form";
        }
        
        // Récupérer le dépôt complet avec son site
        if (inventaire.getDepot() != null && inventaire.getDepot().getId() != null) {
            referentielService.findDepotById(inventaire.getDepot().getId())
                .ifPresent(inventaire::setDepot);
        }
        
        Utilisateur user = getCurrentUser(auth);
        Inventaire saved = inventaireService.createInventaire(inventaire, user);
        redirectAttributes.addFlashAttribute("success", "Inventaire créé avec succès");
        return "redirect:/inventaires/" + saved.getId();
    }
    
    // ============ WORKFLOW ============
    
    @PostMapping("/{id}/start")
    public String demarrerInventaire(@PathVariable UUID id,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            inventaireService.demarrerInventaire(id, user);
            redirectAttributes.addFlashAttribute("success", "Inventaire démarré");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventaires/" + id;
    }
    
    @PostMapping("/{id}/analyze")
    public String passerEnAnalyse(@PathVariable UUID id,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            inventaireService.passerEnAnalyse(id, user);
            redirectAttributes.addFlashAttribute("success", "Inventaire en phase d'analyse");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventaires/" + id;
    }
    
    @PostMapping("/{id}/close")
    public String cloturerInventaire(@PathVariable UUID id,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            inventaireService.cloturerInventaire(id, user);
            redirectAttributes.addFlashAttribute("success", "Inventaire clôturé - Ajustements appliqués");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventaires/" + id;
    }
    
    // ============ SAISIE DES COMPTAGES ============
    
    @GetMapping("/{invId}/lignes/{ligneId}/saisie")
    public String saisieComptageForm(@PathVariable UUID invId,
                                     @PathVariable UUID ligneId,
                                     Model model) {
        model.addAttribute("inventaireId", invId);
        model.addAttribute("ligneId", ligneId);
        return "inventaires/saisie-form";
    }
    
    @PostMapping("/{invId}/lignes/{ligneId}/saisie")
    public String saisirComptage(@PathVariable UUID invId,
                                 @PathVariable UUID ligneId,
                                 @RequestParam BigDecimal qtyComptee,
                                 @RequestParam int tour,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            inventaireService.saisirComptage(ligneId, user, qtyComptee, null, tour);
            redirectAttributes.addFlashAttribute("success", "Comptage enregistré");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventaires/" + invId;
    }
    
    // ============ VALIDATION DES ÉCARTS ============
    
    @PostMapping("/saisies/{saisieId}/retain")
    public String retenirSaisie(@PathVariable UUID saisieId,
                                @RequestParam UUID inventaireId,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            inventaireService.retenirSaisie(saisieId, user);
            redirectAttributes.addFlashAttribute("success", "Saisie retenue");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventaires/" + inventaireId;
    }
    
    @PostMapping("/lignes/{ligneId}/validate")
    public String validerEcart(@PathVariable UUID ligneId,
                               @RequestParam UUID inventaireId,
                               @RequestParam(required = false) String notes,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            inventaireService.validerEcart(ligneId, user, notes);
            redirectAttributes.addFlashAttribute("success", "Écart validé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventaires/" + inventaireId;
    }
}
