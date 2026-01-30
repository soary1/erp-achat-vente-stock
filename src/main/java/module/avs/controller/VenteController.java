package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.Utilisateur;
import module.avs.model.vente.*;
import module.avs.service.ReferentielService;
import module.avs.service.UtilisateurService;
import module.avs.service.VenteService;
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
import java.util.UUID;

@Controller
@RequestMapping("/ventes")
@RequiredArgsConstructor
public class VenteController {
    
    private final VenteService venteService;
    private final ReferentielService referentielService;
    private final UtilisateurService utilisateurService;
    
    private Utilisateur getCurrentUser(Authentication auth) {
        return utilisateurService.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    
    // ============ DEVIS ============
    
    @GetMapping("/devis")
    public String listDevis(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DevisClient> devis = venteService.findAllDevis(pageable);
        model.addAttribute("devis", devis);
        return "ventes/devis";
    }
    
    @GetMapping("/devis/add")
    public String addDevisForm(Model model) {
        model.addAttribute("devis", new DevisClient());
        model.addAttribute("clients", referentielService.findAllClients());
        model.addAttribute("sites", referentielService.findAllSites());
        model.addAttribute("articles", referentielService.findAllArticles());
        return "ventes/devis-form";
    }
    
    @GetMapping("/devis/{id}")
    public String viewDevis(@PathVariable UUID id, Model model) {
        venteService.findDevisById(id).ifPresent(d -> model.addAttribute("devis", d));
        return "ventes/devis-detail";
    }
    
    @PostMapping("/devis/save")
    public String saveDevis(@Valid @ModelAttribute DevisClient devis,
                           BindingResult result,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "ventes/devis-form";
        }
        Utilisateur user = getCurrentUser(auth);
        venteService.createDevis(devis, user);
        redirectAttributes.addFlashAttribute("success", "Devis créé avec succès");
        return "redirect:/ventes/devis";
    }
    
    @PostMapping("/devis/{id}/validate")
    public String validerDevis(@PathVariable UUID id,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            venteService.validerDevis(id, user);
            redirectAttributes.addFlashAttribute("success", "Devis validé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventes/devis/" + id;
    }
    
    @PostMapping("/devis/{id}/transform")
    public String transformerDevis(@PathVariable UUID id,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            CommandeClient commande = venteService.createCommandeFromDevis(id, user);
            redirectAttributes.addFlashAttribute("success", "Commande créée à partir du devis");
            return "redirect:/ventes/commandes/" + commande.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventes/devis/" + id;
    }
    
    // ============ COMMANDES CLIENT ============
    
    @GetMapping("/commandes")
    public String listCommandes(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommandeClient> commandes = venteService.findAllCommandesClient(pageable);
        model.addAttribute("commandes", commandes);
        return "ventes/commandes";
    }
    
    @GetMapping("/commandes/add")
    public String addCommandeForm(Model model) {
        model.addAttribute("commande", new CommandeClient());
        model.addAttribute("clients", referentielService.findAllClients());
        model.addAttribute("sites", referentielService.findAllSites());
        model.addAttribute("articles", referentielService.findAllArticles());
        return "ventes/commande-form";
    }
    
    @GetMapping("/commandes/{id}")
    public String viewCommande(@PathVariable UUID id, Model model) {
        venteService.findCommandeClientById(id).ifPresent(c -> model.addAttribute("commande", c));
        return "ventes/commande-detail";
    }
    
    @PostMapping("/commandes/save")
    public String saveCommande(@Valid @ModelAttribute CommandeClient commande,
                               BindingResult result,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "ventes/commande-form";
        }
        Utilisateur user = getCurrentUser(auth);
        venteService.createCommandeClient(commande, user);
        redirectAttributes.addFlashAttribute("success", "Commande créée avec succès");
        return "redirect:/ventes/commandes";
    }
    
    @PostMapping("/commandes/{id}/confirm")
    public String confirmerCommande(@PathVariable UUID id,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            venteService.confirmerCommande(id, user);
            redirectAttributes.addFlashAttribute("success", "Commande confirmée - Stock réservé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventes/commandes/" + id;
    }
    
    @PostMapping("/commandes/{id}/prepare")
    public String preparerCommande(@PathVariable UUID id,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            venteService.preparerCommande(id, user);
            redirectAttributes.addFlashAttribute("success", "Commande en préparation");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventes/commandes/" + id;
    }
    
    // ============ LIVRAISONS ============
    
    @GetMapping("/livraisons")
    public String listLivraisons(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BonLivraison> livraisons = venteService.findAllLivraisons(pageable);
        model.addAttribute("livraisons", livraisons);
        return "ventes/livraisons";
    }
    
    @GetMapping("/livraisons/{id}")
    public String viewLivraison(@PathVariable UUID id, Model model) {
        venteService.findLivraisonById(id).ifPresent(l -> model.addAttribute("livraison", l));
        return "ventes/livraison-detail";
    }
    
    @GetMapping("/commandes/{cmdId}/livraison/add")
    public String addLivraisonForm(@PathVariable UUID cmdId, Model model) {
        venteService.findCommandeClientById(cmdId).ifPresent(c -> model.addAttribute("commande", c));
        return "ventes/livraison-form";
    }
    
    @PostMapping("/commandes/{cmdId}/livraison")
    public String createLivraison(@PathVariable UUID cmdId,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            BonLivraison bl = venteService.createLivraison(cmdId, user);
            redirectAttributes.addFlashAttribute("success", "Bon de livraison créé");
            return "redirect:/ventes/livraisons/" + bl.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventes/commandes/" + cmdId;
    }
    
    @PostMapping("/livraisons/{id}/validate")
    public String validerLivraison(@PathVariable UUID id,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            venteService.validerLivraison(id, user);
            redirectAttributes.addFlashAttribute("success", "Livraison validée - Stock décrémenté");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventes/livraisons/" + id;
    }
}
