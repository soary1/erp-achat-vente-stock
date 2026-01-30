package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.referentiel.*;
import module.avs.service.ReferentielService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/referentiels")
@RequiredArgsConstructor
public class ReferentielController {
    
    private final ReferentielService referentielService;
    
    // ============ DEVISES ============
    
    @GetMapping("/devises")
    public String listDevises(Model model) {
        model.addAttribute("devises", referentielService.findAllDevises());
        return "referentiels/devises";
    }
    
    @GetMapping("/devises/add")
    public String addDeviseForm(Model model) {
        model.addAttribute("devise", new Devise());
        return "referentiels/devise-form";
    }
    
    @PostMapping("/devises/save")
    public String saveDevise(@Valid @ModelAttribute Devise devise, BindingResult result, 
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "referentiels/devise-form";
        }
        referentielService.saveDevise(devise);
        redirectAttributes.addFlashAttribute("success", "Devise enregistrée avec succès");
        return "redirect:/referentiels/devises";
    }
    
    @GetMapping("/devises/edit/{code}")
    public String editDeviseForm(@PathVariable String code, Model model) {
        referentielService.findDeviseById(code).ifPresent(d -> model.addAttribute("devise", d));
        return "referentiels/devise-form";
    }
    
    @GetMapping("/devises/delete/{code}")
    public String deleteDevise(@PathVariable String code, RedirectAttributes redirectAttributes) {
        referentielService.deleteDevise(code);
        redirectAttributes.addFlashAttribute("success", "Devise supprimée");
        return "redirect:/referentiels/devises";
    }
    
    // ============ PAYS ============
    
    @GetMapping("/pays")
    public String listPays(Model model) {
        model.addAttribute("pays", referentielService.findAllPays());
        return "referentiels/pays";
    }
    
    @GetMapping("/pays/add")
    public String addPaysForm(Model model) {
        model.addAttribute("pays", new Pays());
        model.addAttribute("devises", referentielService.findAllDevises());
        return "referentiels/pays-form";
    }
    
    @PostMapping("/pays/save")
    public String savePays(@Valid @ModelAttribute Pays pays, BindingResult result, 
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "referentiels/pays-form";
        }
        referentielService.savePays(pays);
        redirectAttributes.addFlashAttribute("success", "Pays enregistré avec succès");
        return "redirect:/referentiels/pays";
    }
    
    @GetMapping("/pays/edit/{code}")
    public String editPaysForm(@PathVariable String code, Model model) {
        referentielService.findPaysById(code).ifPresent(p -> model.addAttribute("pays", p));
        model.addAttribute("devises", referentielService.findAllDevises());
        return "referentiels/pays-form";
    }
    
    // ============ UNITES DE MESURE ============
    
    @GetMapping("/unites")
    public String listUnites(Model model) {
        model.addAttribute("unites", referentielService.findAllUnitesMesure());
        return "referentiels/unites";
    }
    
    @GetMapping("/unites/add")
    public String addUniteForm(Model model) {
        model.addAttribute("unite", new UniteMesure());
        return "referentiels/unite-form";
    }
    
    @PostMapping("/unites/save")
    public String saveUnite(@Valid @ModelAttribute("unite") UniteMesure unite, BindingResult result, 
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "referentiels/unite-form";
        }
        referentielService.saveUniteMesure(unite);
        redirectAttributes.addFlashAttribute("success", "Unité enregistrée avec succès");
        return "redirect:/referentiels/unites";
    }
    
    // ============ TYPES DE TAXES ============
    
    @GetMapping("/taxes")
    public String listTaxes(Model model) {
        model.addAttribute("taxes", referentielService.findAllTypesTaxe());
        return "referentiels/taxes";
    }
    
    @GetMapping("/taxes/add")
    public String addTaxeForm(Model model) {
        model.addAttribute("taxe", new TypeTaxe());
        return "referentiels/taxe-form";
    }
    
    @PostMapping("/taxes/save")
    public String saveTaxe(@Valid @ModelAttribute("taxe") TypeTaxe taxe, BindingResult result, 
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "referentiels/taxe-form";
        }
        referentielService.saveTypeTaxe(taxe);
        redirectAttributes.addFlashAttribute("success", "Taxe enregistrée avec succès");
        return "redirect:/referentiels/taxes";
    }
    
    // ============ MODES DE PAIEMENT ============
    
    @GetMapping("/modes-paiement")
    public String listModesPaiement(Model model) {
        model.addAttribute("modes", referentielService.findAllModesPaiement());
        return "referentiels/modes-paiement";
    }
    
    @GetMapping("/modes-paiement/add")
    public String addModePaiementForm(Model model) {
        model.addAttribute("mode", new ModePaiement());
        return "referentiels/mode-paiement-form";
    }
    
    @PostMapping("/modes-paiement/save")
    public String saveModePaiement(@Valid @ModelAttribute("mode") ModePaiement mode, BindingResult result, 
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "referentiels/mode-paiement-form";
        }
        referentielService.saveModePaiement(mode);
        redirectAttributes.addFlashAttribute("success", "Mode de paiement enregistré avec succès");
        return "redirect:/referentiels/modes-paiement";
    }
}
