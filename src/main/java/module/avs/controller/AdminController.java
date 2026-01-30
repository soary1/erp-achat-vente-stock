package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.security.*;
import module.avs.service.UtilisateurService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UtilisateurService utilisateurService;
    
    // ============ UTILISATEURS ============
    
    @GetMapping("/utilisateurs")
    public String listUtilisateurs(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Utilisateur> utilisateurs = utilisateurService.findAllUtilisateurs(pageable);
        model.addAttribute("utilisateurs", utilisateurs);
        return "admin/utilisateurs";
    }
    
    @GetMapping("/utilisateurs/add")
    public String addUtilisateurForm(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        model.addAttribute("roles", utilisateurService.findAllRoles());
        model.addAttribute("departements", utilisateurService.findAllDepartements());
        return "admin/utilisateur-form";
    }
    
    @GetMapping("/utilisateurs/{id}")
    public String viewUtilisateur(@PathVariable UUID id, Model model) {
        utilisateurService.findById(id).ifPresent(u -> model.addAttribute("utilisateur", u));
        return "admin/utilisateur-detail";
    }
    
    @GetMapping("/utilisateurs/{id}/edit")
    public String editUtilisateurForm(@PathVariable UUID id, Model model) {
        utilisateurService.findById(id).ifPresent(u -> model.addAttribute("utilisateur", u));
        model.addAttribute("roles", utilisateurService.findAllRoles());
        model.addAttribute("departements", utilisateurService.findAllDepartements());
        return "admin/utilisateur-form";
    }
    
    @PostMapping("/utilisateurs/save")
    public String saveUtilisateur(@Valid @ModelAttribute Utilisateur utilisateur,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/utilisateur-form";
        }
        utilisateurService.save(utilisateur);
        redirectAttributes.addFlashAttribute("success", "Utilisateur enregistré");
        return "redirect:/admin/utilisateurs";
    }
    
    @PostMapping("/utilisateurs/{id}/toggle")
    public String toggleUtilisateur(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        utilisateurService.findById(id).ifPresent(u -> {
            u.setIsActive(!u.getIsActive());
            utilisateurService.save(u);
        });
        redirectAttributes.addFlashAttribute("success", "Statut modifié");
        return "redirect:/admin/utilisateurs";
    }
    
    // ============ RÔLES ============
    
    @GetMapping("/roles")
    public String listRoles(Model model) {
        model.addAttribute("roles", utilisateurService.findAllRoles());
        return "admin/roles";
    }
    
    @GetMapping("/roles/add")
    public String addRoleForm(Model model) {
        model.addAttribute("role", new Role());
        return "admin/role-form";
    }
    
    @PostMapping("/roles/save")
    public String saveRole(@Valid @ModelAttribute Role role,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/role-form";
        }
        utilisateurService.saveRole(role);
        redirectAttributes.addFlashAttribute("success", "Rôle enregistré");
        return "redirect:/admin/roles";
    }
    
    // ============ DÉPARTEMENTS ============
    
    @GetMapping("/departements")
    public String listDepartements(Model model) {
        model.addAttribute("departements", utilisateurService.findAllDepartements());
        return "admin/departements";
    }
    
    @GetMapping("/departements/add")
    public String addDepartementForm(Model model) {
        model.addAttribute("departement", new Departement());
        return "admin/departement-form";
    }
    
    @PostMapping("/departements/save")
    public String saveDepartement(@Valid @ModelAttribute Departement departement,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/departement-form";
        }
        utilisateurService.saveDepartement(departement);
        redirectAttributes.addFlashAttribute("success", "Département enregistré");
        return "redirect:/admin/departements";
    }
    
    // ============ AUDIT ============
    
    @GetMapping("/audit")
    public String listAuditLogs(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size,
                                Model model) {
        Pageable pageable = PageRequest.of(page, size);
        model.addAttribute("logs", utilisateurService.getAuditLogs(pageable));
        return "admin/audit";
    }
    
    // ============ RÈGLES D'APPROBATION ============
    
    @GetMapping("/regles-approbation")
    public String listReglesApprobation(Model model) {
        model.addAttribute("regles", utilisateurService.findAllReglesApprobation());
        return "admin/regles-approbation";
    }
    
    @GetMapping("/regles-approbation/add")
    public String addRegleForm(Model model) {
        model.addAttribute("regle", new RegleApprobation());
        model.addAttribute("roles", utilisateurService.findAllRoles());
        return "admin/regle-approbation-form";
    }
    
    @PostMapping("/regles-approbation/save")
    public String saveRegleApprobation(@Valid @ModelAttribute RegleApprobation regle,
                                       BindingResult result,
                                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/regle-approbation-form";
        }
        utilisateurService.saveRegleApprobation(regle);
        redirectAttributes.addFlashAttribute("success", "Règle enregistrée");
        return "redirect:/admin/regles-approbation";
    }
}
