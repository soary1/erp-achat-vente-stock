package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.repository.organisation.DepotRepository;
import module.avs.repository.organisation.SiteRepository;
import module.avs.repository.tiers.ClientRepository;
import module.avs.repository.tiers.FournisseurRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/carte")
@RequiredArgsConstructor
public class CarteController {

    private final SiteRepository siteRepository;
    private final DepotRepository depotRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;

    @GetMapping
    public String index(Model model) {
        return "carte/index";
    }

    @GetMapping("/sites")
    public String sites(Model model) {
        model.addAttribute("sites", siteRepository.findByIsActiveTrue());
        return "carte/sites";
    }

    @GetMapping("/depots")
    public String depots(Model model) {
        model.addAttribute("depots", depotRepository.findByIsActiveTrue());
        return "carte/depots";
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
