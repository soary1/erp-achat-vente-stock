package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.model.organisation.Depot;
import module.avs.model.organisation.Site;
import module.avs.model.stock.Stock;
import module.avs.repository.organisation.DepotRepository;
import module.avs.repository.organisation.SiteRepository;
import module.avs.repository.stock.StockRepository;
import module.avs.repository.tiers.ClientRepository;
import module.avs.repository.tiers.FournisseurRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/carte")
@RequiredArgsConstructor
public class CarteController {

    private final SiteRepository siteRepository;
    private final DepotRepository depotRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final StockRepository stockRepository;

    @GetMapping
    public String index(Model model) {
        return "carte/index";
    }

    @GetMapping("/sites")
    public String sites(Model model) {
        model.addAttribute("sites", siteRepository.findByIsActiveTrue());
        return "carte/sites";
    }

    @GetMapping("/sites/{id}")
    public String siteDetail(@PathVariable UUID id, Model model) {
        Site site = siteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Site non trouvé"));
        
        List<Depot> depots = depotRepository.findBySiteId(id);
        
        // Calculer le stock total par dépôt
        Map<UUID, BigDecimal> stockTotalParDepot = depots.stream()
            .collect(Collectors.toMap(
                Depot::getId,
                depot -> {
                    BigDecimal total = stockRepository.getTotalStockByDepot(depot.getId());
                    return total != null ? total : BigDecimal.ZERO;
                }
            ));
        
        model.addAttribute("site", site);
        model.addAttribute("depots", depots);
        model.addAttribute("stockTotalParDepot", stockTotalParDepot);
        
        return "carte/site-detail";
    }

    @GetMapping("/depots")
    public String depots(Model model) {
        model.addAttribute("depots", depotRepository.findByIsActiveTrue());
        return "carte/depots";
    }

    @GetMapping("/depots/{id}")
    public String depotDetail(@PathVariable UUID id, Model model) {
        Depot depot = depotRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dépôt non trouvé"));
        
        List<Stock> stocks = stockRepository.findByDepotId(id);
        
        // Calculer les totaux
        BigDecimal stockTotal = stocks.stream()
            .map(Stock::getQtyReel)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal stockReserve = stocks.stream()
            .map(Stock::getQtyReserve)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal stockDisponible = stocks.stream()
            .map(Stock::getQtyDisponible)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Nombre d'articles distincts
        long nbArticles = stocks.stream()
            .map(stock -> stock.getArticle().getId())
            .distinct()
            .count();
        
        model.addAttribute("depot", depot);
        model.addAttribute("stocks", stocks);
        model.addAttribute("stockTotal", stockTotal);
        model.addAttribute("stockReserve", stockReserve);
        model.addAttribute("stockDisponible", stockDisponible);
        model.addAttribute("nbArticles", nbArticles);
        
        return "carte/depot-detail";
    }

    @GetMapping("/clients")
    public String clients(Model model) {
        model.addAttribute("clients", clientRepository.findByIsActiveTrue());
        return "carte/clients";
    }

    @GetMapping("/fournisseurs")
    public String fournisseurs(Model model) {
        model.addAttribute("fournisseurs", fournisseurRepository.findByIsActiveTrue());
        return "carte/fournisseurs";
    }
}
