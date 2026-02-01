package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.finance.*;
import module.avs.model.security.Utilisateur;
import module.avs.model.vente.CommandeClient;
import module.avs.service.AchatService;
import module.avs.service.FinanceService;
import module.avs.service.ReferentielService;
import module.avs.service.UtilisateurService;
import module.avs.service.VenteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {
    
    private final FinanceService financeService;
    private final VenteService venteService;
    private final AchatService achatService;
    private final ReferentielService referentielService;
    private final UtilisateurService utilisateurService;
    
    private Utilisateur getCurrentUser(Authentication auth) {
        return utilisateurService.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
    
    // ============ TABLEAU DE BORD ============
    
    @GetMapping
    public String dashboard(Model model) {
        Map<String, Object> stats = financeService.getDashboardStats();
        model.addAllAttributes(stats);
        
        // Pour compatibilité avec l'ancien template
        model.addAttribute("facturesClientImpayees", stats.get("creancesClient"));
        model.addAttribute("facturesFournisseurImpayees", stats.get("dettesFournisseur"));
        model.addAttribute("facturesClientRetard", stats.get("facturesClientRetard"));
        model.addAttribute("facturesFournisseurRetard", stats.get("facturesFournisseurRetard"));
        
        return "finance/dashboard";
    }
    
    // ============ FACTURES FOURNISSEUR ============
    
    @GetMapping("/factures-fournisseur")
    public String listFacturesFournisseur(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FactureFournisseur> factures = financeService.findAllFacturesFournisseur(pageable);
        model.addAttribute("factures", factures);
        return "finance/factures-fournisseur";
    }
    
    @GetMapping("/factures-fournisseur/{id}")
    public String viewFactureFournisseur(@PathVariable UUID id, Model model) {
        financeService.findFactureFournisseurByIdWithDetails(id).ifPresent(f -> {
            model.addAttribute("facture", f);
            model.addAttribute("paiements", financeService.findPaiementsByFactureFournisseur(id));
            // Récupérer les lignes de commande si disponibles
            if (f.getCommandeAchat() != null) {
                achatService.findCommandeAchatById(f.getCommandeAchat().getId())
                    .ifPresent(cmd -> model.addAttribute("commande", cmd));
            }
        });
        model.addAttribute("modes", referentielService.findAllModesPaiement());
        return "finance/facture-fournisseur-detail";
    }
    
    @GetMapping("/factures-fournisseur/add")
    public String addFactureFournisseurForm(Model model) {
        model.addAttribute("facture", new FactureFournisseur());
        model.addAttribute("fournisseurs", referentielService.findAllFournisseurs());
        return "finance/facture-fournisseur-form";
    }
    
    @PostMapping("/factures-fournisseur/save")
    public String saveFactureFournisseur(@ModelAttribute FactureFournisseur facture,
                                         Authentication auth,
                                         RedirectAttributes redirectAttributes) {
        Utilisateur user = getCurrentUser(auth);
        financeService.createFactureFournisseur(facture, user);
        redirectAttributes.addFlashAttribute("success", "Facture enregistrée");
        return "redirect:/finance/factures-fournisseur";
    }
    
    // ============ RAPPROCHEMENT 3-WAY ============
    
    @GetMapping("/factures-fournisseur/{id}/rapprochement")
    public String rapprochementForm(@PathVariable UUID id, Model model) {
        financeService.findFactureFournisseurById(id).ifPresent(f -> {
            model.addAttribute("facture", f);
            model.addAttribute("receptions", referentielService.findReceptionsNonRapprochees());
        });
        return "finance/rapprochement-form";
    }
    
    @PostMapping("/factures-fournisseur/{id}/rapprochement")
    public String effectuerRapprochement(@PathVariable UUID id,
                                         @RequestParam UUID receptionId,
                                         RedirectAttributes redirectAttributes) {
        try {
            FinanceService.RapprochementResult result = financeService.effectuerRapprochement(id, receptionId);
            if (result.isMatch()) {
                redirectAttributes.addFlashAttribute("success", "Rapprochement validé");
            } else {
                redirectAttributes.addFlashAttribute("warning", 
                    String.format("Écart détecté : BC=%.2f, Réception=%.2f, Facture=%.2f", 
                        result.montantCommande(), result.montantReception(), result.montantFacture()));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/finance/factures-fournisseur/" + id;
    }
    
    // ============ PAIEMENTS FOURNISSEUR ============
    
    @GetMapping("/factures-fournisseur/{id}/paiement")
    public String paiementFournisseurForm(@PathVariable UUID id, Model model) {
        financeService.findFactureFournisseurById(id).ifPresent(f -> {
            model.addAttribute("facture", f);
            model.addAttribute("modes", referentielService.findAllModesPaiement());
        });
        return "finance/paiement-fournisseur-form";
    }
    
    @PostMapping("/factures-fournisseur/{id}/paiement")
    public String enregistrerPaiementFournisseur(@PathVariable UUID id,
                                                 @RequestParam BigDecimal montant,
                                                 @RequestParam String modeCode,
                                                 @RequestParam(required = false) String reference,
                                                 Authentication auth,
                                                 RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            FactureFournisseur facture = financeService.findFactureFournisseurById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
            
            var modePaiement = referentielService.findModePaiementByCode(modeCode)
                .orElseThrow(() -> new RuntimeException("Mode de paiement non trouvé"));
            
            PaiementFournisseur paiement = PaiementFournisseur.builder()
                .facture(facture)
                .montant(montant)
                .modePaiement(modePaiement)
                .reference(reference)
                .datePaiement(LocalDate.now())
                .build();
            
            financeService.enregistrerPaiement(paiement, user);
            redirectAttributes.addFlashAttribute("success", "Paiement enregistré");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/finance/factures-fournisseur/" + id;
    }
    
    // ============ FACTURES CLIENT ============
    
    @GetMapping("/factures-client")
    public String listFacturesClient(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FactureClient> factures = financeService.findAllFacturesClient(pageable);
        model.addAttribute("factures", factures);
        return "finance/factures-client";
    }
    
    @GetMapping("/factures-client/{id}")
    public String viewFactureClient(@PathVariable UUID id, Model model) {
        financeService.findFactureClientByIdWithDetails(id).ifPresent(f -> {
            model.addAttribute("facture", f);
            model.addAttribute("encaissements", financeService.findEncaissementsByFactureClient(id));
            // Récupérer les détails de la commande et du BL si disponibles
            if (f.getCommande() != null) {
                venteService.findCommandeClientById(f.getCommande().getId())
                    .ifPresent(cmd -> model.addAttribute("commande", cmd));
            }
            if (f.getBonLivraison() != null) {
                venteService.findLivraisonById(f.getBonLivraison().getId())
                    .ifPresent(bl -> model.addAttribute("bonLivraison", bl));
            }
        });
        model.addAttribute("modes", referentielService.findAllModesPaiement());
        return "finance/facture-client-detail";
    }
    
    @PostMapping("/commandes/{cmdId}/facturer")
    public String creerFactureClient(@PathVariable UUID cmdId,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            CommandeClient commande = venteService.findCommandeClientById(cmdId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
            
            FactureClient facture = financeService.createFactureClient(commande, user);
            redirectAttributes.addFlashAttribute("success", "Facture créée");
            return "redirect:/finance/factures-client/" + facture.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventes/commandes/" + cmdId;
    }
    
    // ============ ENCAISSEMENTS ============
    
    @GetMapping("/factures-client/{id}/encaissement")
    public String encaissementForm(@PathVariable UUID id, Model model) {
        financeService.findFactureClientById(id).ifPresent(f -> {
            model.addAttribute("facture", f);
            model.addAttribute("modes", referentielService.findAllModesPaiement());
        });
        return "finance/encaissement-form";
    }
    
    @PostMapping("/factures-client/{id}/encaissement")
    public String enregistrerEncaissement(@PathVariable UUID id,
                                          @RequestParam BigDecimal montant,
                                          @RequestParam String modeCode,
                                          @RequestParam(required = false) String reference,
                                          Authentication auth,
                                          RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            FactureClient facture = financeService.findFactureClientById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
            
            var modePaiement = referentielService.findModePaiementByCode(modeCode)
                .orElseThrow(() -> new RuntimeException("Mode de paiement non trouvé"));
            
            EncaissementClient encaissement = EncaissementClient.builder()
                .facture(facture)
                .montant(montant)
                .modePaiement(modePaiement)
                .reference(reference)
                .dateEncaissement(LocalDate.now())
                .build();
            
            financeService.enregistrerEncaissement(encaissement, user);
            redirectAttributes.addFlashAttribute("success", "Encaissement enregistré");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/finance/factures-client/" + id;
    }
}
