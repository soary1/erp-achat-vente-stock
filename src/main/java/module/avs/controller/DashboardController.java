package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, Authentication authentication) {
        // KPIs généraux
        Map<String, Object> kpisDirection = dashboardService.getKPIsDirection();
        model.addAttribute("kpisDirection", kpisDirection);
        
        // KPIs par module
        model.addAttribute("kpisAchats", dashboardService.getKPIsAchats());
        model.addAttribute("kpisVentes", dashboardService.getKPIsVentes());
        model.addAttribute("kpisStock", dashboardService.getKPIsStock());
        model.addAttribute("kpisFinance", dashboardService.getKPIsFinance());
        
        // Données pour graphiques
        model.addAttribute("evolutionCA", dashboardService.getEvolutionCA(6));
        model.addAttribute("repartitionStatuts", dashboardService.getRepartitionStatutsCommandes());
        
        return "dashboard";
    }
    
    @GetMapping("/dashboard/achats")
    public String dashboardAchats(Model model) {
        model.addAttribute("kpis", dashboardService.getKPIsAchats());
        return "dashboard-achats";
    }
    
    @GetMapping("/dashboard/ventes")
    public String dashboardVentes(Model model) {
        model.addAttribute("kpis", dashboardService.getKPIsVentes());
        return "dashboard-ventes";
    }
    
    @GetMapping("/dashboard/stock")
    public String dashboardStock(Model model) {
        model.addAttribute("kpis", dashboardService.getKPIsStock());
        return "dashboard-stock";
    }
    
    @GetMapping("/dashboard/finance")
    public String dashboardFinance(Model model) {
        model.addAttribute("kpis", dashboardService.getKPIsFinance());
        return "dashboard-finance";
    }
}
