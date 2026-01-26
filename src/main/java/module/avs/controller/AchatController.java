package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.achat.*;
import module.avs.model.security.Utilisateur;
import module.avs.service.AchatService;
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
import java.util.UUID;

@Controller
@RequestMapping("/achats")
@RequiredArgsConstructor
public class AchatController {
    
    private final AchatService achatService;
    private final ReferentielService referentielService;
    private final UtilisateurService utilisateurService;
    
    private Utilisateur getCurrentUser(Authentication auth) {
        return utilisateurService.findByLogin(auth.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    
    // ============ DEMANDES D'ACHAT ============
    
    @GetMapping("/demandes")
    public String listDemandes(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DemandeAchat> demandes = achatService.findAllDemandesAchat(pageable);
        model.addAttribute("demandes", demandes);
        return "achats/demandes";
    }
    
    @GetMapping("/demandes/add")
    public String addDemandeForm(Model model) {
        model.addAttribute("demande", new DemandeAchat());
        model.addAttribute("sites", referentielService.findAllSites());
        return "achats/demande-form";
    }
    
    @GetMapping("/demandes/{id}")
    public String viewDemande(@PathVariable UUID id, Model model) {
        achatService.findDemandeAchatById(id).ifPresent(d -> model.addAttribute("demande", d));
        return "achats/demande-detail";
    }
    
    @PostMapping("/demandes/save")
    public String saveDemande(@Valid @ModelAttribute DemandeAchat demande, 
                              BindingResult result,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "achats/demande-form";
        }
        Utilisateur user = getCurrentUser(auth);
        achatService.createDemandeAchat(demande, user);
        redirectAttributes.addFlashAttribute("success", "Demande d'achat créée avec succès");
        return "redirect:/achats/demandes";
    }
    
    @PostMapping("/demandes/{id}/submit")
    public String soumettreDemande(@PathVariable UUID id, 
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            achatService.soumettreDemande(id, user);
            redirectAttributes.addFlashAttribute("success", "Demande soumise pour validation");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/achats/demandes/" + id;
    }
    
    @PostMapping("/demandes/{id}/approve")
    public String approuverDemande(@PathVariable UUID id, 
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            achatService.approuverDemande(id, user);
            redirectAttributes.addFlashAttribute("success", "Demande approuvée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/achats/demandes/" + id;
    }
    
    @PostMapping("/demandes/{id}/reject")
    public String rejeterDemande(@PathVariable UUID id,
                                 @RequestParam String motif,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            achatService.rejeterDemande(id, user, motif);
            redirectAttributes.addFlashAttribute("success", "Demande rejetée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/achats/demandes/" + id;
    }
    
    // ============ COMMANDES D'ACHAT ============
    
    @GetMapping("/commandes")
    public String listCommandes(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommandeAchat> commandes = achatService.findAllCommandesAchat(pageable);
        model.addAttribute("commandes", commandes);
        return "achats/commandes";
    }
    
    @GetMapping("/commandes/add")
    public String addCommandeForm(Model model) {
        model.addAttribute("commande", new CommandeAchat());
        model.addAttribute("fournisseurs", referentielService.findAllFournisseurs());
        model.addAttribute("sites", referentielService.findAllSites());
        model.addAttribute("devises", referentielService.findAllDevises());
        return "achats/commande-form";
    }
    
    @GetMapping("/commandes/{id}")
    public String viewCommande(@PathVariable UUID id, Model model) {
        achatService.findCommandeAchatById(id).ifPresent(c -> model.addAttribute("commande", c));
        return "achats/commande-detail";
    }
    
    @PostMapping("/commandes/save")
    public String saveCommande(@Valid @ModelAttribute CommandeAchat commande,
                               BindingResult result,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "achats/commande-form";
        }
        Utilisateur user = getCurrentUser(auth);
        achatService.createCommandeAchat(commande, user);
        redirectAttributes.addFlashAttribute("success", "Commande créée avec succès");
        return "redirect:/achats/commandes";
    }
    
    @PostMapping("/commandes/{id}/submit")
    public String soumettreCommande(@PathVariable UUID id, 
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            achatService.soumettreCommande(id, user);
            redirectAttributes.addFlashAttribute("success", "Commande soumise pour validation");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/achats/commandes/" + id;
    }
    
    @PostMapping("/commandes/{id}/approve")
    public String approuverCommande(@PathVariable UUID id, 
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            achatService.approuverCommande(id, user);
            redirectAttributes.addFlashAttribute("success", "Commande approuvée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/achats/commandes/" + id;
    }
    
    @PostMapping("/commandes/{id}/reject")
    public String rejeterCommande(@PathVariable UUID id,
                                  @RequestParam String motif,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            achatService.rejeterCommande(id, user, motif);
            redirectAttributes.addFlashAttribute("success", "Commande rejetée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/achats/commandes/" + id;
    }
    
    @PostMapping("/commandes/{id}/send")
    public String envoyerCommande(@PathVariable UUID id, 
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            achatService.envoyerCommande(id, user);
            redirectAttributes.addFlashAttribute("success", "Commande envoyée au fournisseur");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/achats/commandes/" + id;
    }
    
    // ============ DEMANDES À APPROUVER ============
    
    @GetMapping("/a-approuver")
    public String demandesAApprouver(Authentication auth, Model model) {
        Utilisateur user = getCurrentUser(auth);
        model.addAttribute("demandes", achatService.findDemandesByStatut("SOUMISE"));
        model.addAttribute("commandes", achatService.findCommandesByStatut("SOUMISE"));
        return "achats/a-approuver";
    }
}
