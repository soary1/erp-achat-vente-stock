package module.avs.controller;

import lombok.RequiredArgsConstructor;
import module.avs.dto.StockTransfertDTO;
import module.avs.dto.StockValorisationDTO;
import module.avs.model.security.Utilisateur;
import module.avs.model.stock.*;
import module.avs.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {
    
    private final StockService stockService;
    private final TransfertStockService transfertStockService;
    private final SortieStockService sortieStockService;
    private final AjustementStockService ajustementStockService;
    private final ReferentielService referentielService;
    private final UtilisateurService utilisateurService;
    private final PrixArticleService prixArticleService;
    
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
                           @RequestParam(required = false) String depot,
                           @RequestParam(required = false) String famille,
                           @RequestParam(required = false) String valorisation,
                           @RequestParam(required = false) String search,
                           Model model) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Convertir les chaînes vides en null pour les UUIDs
        UUID depotId = (depot != null && !depot.trim().isEmpty()) ? UUID.fromString(depot) : null;
        UUID familleId = (famille != null && !famille.trim().isEmpty()) ? UUID.fromString(famille) : null;
        String methodeCode = (valorisation != null && !valorisation.trim().isEmpty()) ? valorisation : null;
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search : null;
        
        // Filtrer les stocks
        List<Stock> allStocks = stockService.findStocksFiltered(depotId, familleId, methodeCode, searchTerm);
        
        // Calculer la valorisation pour tous les stocks (pour le total)
        BigDecimal valeurTotale = BigDecimal.ZERO;
        for (Stock stock : allStocks) {
            BigDecimal valeurStock = prixArticleService.getValeurStock(stock);
            valeurTotale = valeurTotale.add(valeurStock);
        }
        
        // Pagination manuelle de la liste filtrée
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allStocks.size());
        List<Stock> pageContent = start < allStocks.size() ? allStocks.subList(start, end) : List.of();
        
        // Convertir en DTOs avec valorisation
        List<StockValorisationDTO> stocksValorisees = pageContent.stream()
            .map(stock -> {
                BigDecimal prixUnitaire = prixArticleService.getCoutUnitaireStock(stock);
                return StockValorisationDTO.fromStock(stock, prixUnitaire);
            })
            .collect(Collectors.toList());
        
        // Créer la page avec les DTOs
        Page<StockValorisationDTO> stocksPage = new org.springframework.data.domain.PageImpl<>(
            stocksValorisees, pageable, allStocks.size());
        
        model.addAttribute("stocks", stocksPage);
        model.addAttribute("valeurTotale", valeurTotale);
        
        // Données pour les filtres
        model.addAttribute("depots", referentielService.findAllDepots());
        model.addAttribute("familles", referentielService.findAllFamilles());
        model.addAttribute("methodeValorisations", referentielService.findAllMethodesValorisation());
        
        // Valeurs sélectionnées pour conserver les filtres
        model.addAttribute("selectedDepot", depotId);
        model.addAttribute("selectedFamille", familleId);
        model.addAttribute("selectedValorisation", methodeCode);
        model.addAttribute("selectedSearch", searchTerm);
        
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
    
    @PostMapping("/lots/controle-qc")
    public String controleQualiteLot(@RequestParam UUID lotId,
                                     @RequestParam boolean conforme,
                                     @RequestParam(required = false) String notes,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            stockService.updateLotQualite(lotId, conforme, notes, user);
            redirectAttributes.addFlashAttribute("success", 
                "Contrôle qualité enregistré : " + (conforme ? "Lot CONFORME" : "Lot REJETÉ"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/stock/lots";
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
    public String addReceptionForm(@RequestParam(required = false) UUID commandeId,
                                   Model model) {
        model.addAttribute("reception", new BonReception());
        model.addAttribute("commandesEnvoyees", referentielService.findCommandesAchatEnAttente());
        model.addAttribute("depots", referentielService.findAllDepots());
        model.addAttribute("emplacements", referentielService.findAllEmplacementsForForm());
        
        // Si une commande est spécifiée, la pré-sélectionner
        if (commandeId != null) {
            model.addAttribute("selectedCommandeId", commandeId);
        }
        
        return "stock/reception-form";
    }
    
    @PostMapping("/receptions/save")
    public String saveReception(@ModelAttribute("reception") module.avs.dto.BonReceptionDTO receptionDTO,
                               BindingResult result,
                               @RequestParam(required = false) String action,
                               Model model,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        
        // Vérifications manuelles car @Valid n'est pas utilisé
        if (receptionDTO.getDepotId() == null) {
            model.addAttribute("error", "Le dépôt de destination est obligatoire");
            model.addAttribute("commandesEnvoyees", referentielService.findCommandesAchatEnAttente());
            model.addAttribute("depots", referentielService.findAllDepots());
            model.addAttribute("emplacements", referentielService.findAllEmplacements());
            model.addAttribute("reception", receptionDTO);
            return "stock/reception-form";
        }
        
        if (receptionDTO.getLignes() == null || receptionDTO.getLignes().isEmpty()) {
            model.addAttribute("error", "Aucune ligne de réception. Veuillez sélectionner une commande.");
            model.addAttribute("commandesEnvoyees", referentielService.findCommandesAchatEnAttente());
            model.addAttribute("depots", referentielService.findAllDepots());
            model.addAttribute("emplacements", referentielService.findAllEmplacements());
            model.addAttribute("reception", receptionDTO);
            return "stock/reception-form";
        }
        
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
            model.addAttribute("reception", receptionDTO);
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
    
    // ============ TRANSFERTS INTER-DÉPÔTS ============
    
    @GetMapping("/transferts")
    public String listTransferts(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransfertStock> transferts = transfertStockService.findAllTransferts(pageable);
        model.addAttribute("transferts", transferts);
        
        // Statistiques
        model.addAttribute("enDemande", transfertStockService.findTransfertsByStatut("DEMANDE").size());
        model.addAttribute("enTransit", transfertStockService.findTransfertsByStatut("EN_TRANSIT").size());
        
        return "stock/transferts";
    }
    
    @GetMapping("/transferts/{id}")
    public String viewTransfert(@PathVariable UUID id, Model model) {
        transfertStockService.findTransfertById(id).ifPresent(t -> model.addAttribute("transfert", t));
        return "stock/transfert-detail";
    }
    
    @GetMapping("/transferts/add")
    public String addTransfertForm(Model model) {
        model.addAttribute("transfert", new TransfertStock());
        model.addAttribute("depots", referentielService.findAllDepots());
        model.addAttribute("articles", referentielService.findAllArticles());
        // Removed: model.addAttribute("stocks", stockService.findAllStocksForTransfer());
        return "stock/transfert-form";
    }
    
    @GetMapping("/api/stocks-by-depot/{depotId}")
    @ResponseBody
    public ResponseEntity<List<StockTransfertDTO>> getStocksByDepot(@PathVariable UUID depotId) {
        List<StockTransfertDTO> stocks = stockService.findStocksByDepot(depotId);
        return ResponseEntity.ok(stocks);
    }
    
    @GetMapping("/api/emplacements-by-depot/{depotId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getEmplacementsByDepot(@PathVariable UUID depotId) {
        List<Map<String, Object>> emplacements = referentielService.findEmplacementsByDepot(depotId)
            .stream()
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", e.getId());
                map.put("code", e.getCode());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(emplacements);
    }
    
    @PostMapping("/transferts/save")
    public String saveTransfert(@RequestParam UUID depotSourceId,
                               @RequestParam UUID depotDestinationId,
                               @RequestParam(required = false) String motif,
                               @RequestParam(required = false) List<UUID> articleIds,
                               @RequestParam(required = false) List<BigDecimal> quantites,
                               @RequestParam(required = false) List<UUID> lotIds,
                               @RequestParam(required = false) List<UUID> emplacementIds,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            TransfertStock saved = transfertStockService.createAndExecuteTransfert(
                depotSourceId, depotDestinationId, motif, 
                articleIds, quantites, lotIds, emplacementIds, user);
            redirectAttributes.addFlashAttribute("success", "Transfert effectué avec succès");
            return "redirect:/stock/transferts/" + saved.getId();
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/stock/transferts/add";
        }
    }
    
    @PostMapping("/transferts/{id}/approuver")
    public String approuverTransfert(@PathVariable UUID id,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            transfertStockService.approuverTransfert(id, user);
            redirectAttributes.addFlashAttribute("success", "Transfert approuvé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/transferts/" + id;
    }
    
    @PostMapping("/transferts/{id}/expedier")
    public String expedierTransfert(@PathVariable UUID id,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            transfertStockService.expedierTransfert(id, user);
            redirectAttributes.addFlashAttribute("success", "Transfert expédié - Stock source décrémenté");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/transferts/" + id;
    }
    
    @PostMapping("/transferts/{id}/recevoir")
    public String recevoirTransfert(@PathVariable UUID id,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            transfertStockService.recevoirTransfert(id, user);
            redirectAttributes.addFlashAttribute("success", "Transfert réceptionné - Stock destination incrémenté");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/transferts/" + id;
    }
    
    @PostMapping("/transferts/{id}/annuler")
    public String annulerTransfert(@PathVariable UUID id,
                                  @RequestParam String motif,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            transfertStockService.annulerTransfert(id, user, motif);
            redirectAttributes.addFlashAttribute("success", "Transfert annulé");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/transferts/" + id;
    }
    
    @PostMapping("/stocks/{id}/changer-emplacement")
    public String changerEmplacement(@PathVariable UUID id,
                                    @RequestParam UUID nouvelEmplacementId,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            transfertStockService.transfererEmplacement(id, nouvelEmplacementId, user);
            redirectAttributes.addFlashAttribute("success", "Emplacement modifié");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock";
    }
    
    // ============ CONSOMMATION INTERNE / REBUT ============
    
    @GetMapping("/sorties")
    public String listSorties(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String type,
                             Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DemandeSortieStock> sorties = type != null 
            ? sortieStockService.findDemandesByType(type, pageable)
            : sortieStockService.findAllDemandes(pageable);
        
        model.addAttribute("sorties", sorties);
        model.addAttribute("typeFiltre", type);
        
        // Statistiques
        model.addAttribute("consommationEnAttente", sortieStockService.countConsommationByStatut("SOUMISE"));
        model.addAttribute("rebutEnAttente", sortieStockService.countRebutByStatut("SOUMISE"));
        
        return "stock/sorties";
    }
    
    @GetMapping("/sorties/{id}")
    public String viewSortie(@PathVariable UUID id, Model model) {
        sortieStockService.findDemandeById(id).ifPresent(s -> model.addAttribute("sortie", s));
        return "stock/sortie-detail";
    }
    
    @GetMapping("/sorties/add")
    public String addSortieForm(@RequestParam String type, Model model) {
        DemandeSortieStock demande = new DemandeSortieStock();
        demande.setType(type);
        
        model.addAttribute("sortie", demande);
        model.addAttribute("depots", referentielService.findAllDepots());
        model.addAttribute("motifs", referentielService.findMotifsByType(type));
        model.addAttribute("articles", referentielService.findAllArticles());
        
        return "stock/sortie-form";
    }
    
    @PostMapping("/sorties/save")
    public String saveSortie(@ModelAttribute("sortie") DemandeSortieStock sortie,
                            Authentication auth,
                            RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            DemandeSortieStock saved = sortieStockService.createDemande(sortie, user);
            redirectAttributes.addFlashAttribute("success", "Demande créée");
            return "redirect:/stock/sorties/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/stock/sorties/add?type=" + sortie.getType();
        }
    }
    
    @PostMapping("/sorties/{id}/soumettre")
    public String soumettreSortie(@PathVariable UUID id,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            sortieStockService.soumettreDemande(id, user);
            redirectAttributes.addFlashAttribute("success", "Demande soumise pour approbation");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/sorties/" + id;
    }
    
    @PostMapping("/sorties/{id}/approuver")
    public String approuverSortie(@PathVariable UUID id,
                                 @RequestParam(required = false) String commentaire,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            sortieStockService.approuverDemande(id, user, commentaire);
            redirectAttributes.addFlashAttribute("success", "Demande approuvée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/sorties/" + id;
    }
    
    @PostMapping("/sorties/{id}/rejeter")
    public String rejeterSortie(@PathVariable UUID id,
                               @RequestParam String motif,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            sortieStockService.rejeterDemande(id, user, motif);
            redirectAttributes.addFlashAttribute("success", "Demande rejetée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/sorties/" + id;
    }
    
    @PostMapping("/sorties/{id}/executer")
    public String executerSortie(@PathVariable UUID id,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            sortieStockService.executerDemande(id, user);
            redirectAttributes.addFlashAttribute("success", "Sortie exécutée - Stock décrémenté");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/sorties/" + id;
    }
    
    // ============ AJUSTEMENTS STOCK ============
    
    @GetMapping("/ajustements")
    public String listAjustements(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AjustementStock> ajustements = ajustementStockService.findAllAjustements(pageable);
        
        model.addAttribute("ajustements", ajustements);
        model.addAttribute("enAttente", ajustementStockService.findAjustementsByStatut("SOUMIS").size());
        model.addAttribute("doubleValidation", ajustementStockService.findAjustementsEnAttenteDoubleValidation().size());
        model.addAttribute("seuilDoubleValidation", ajustementStockService.getSeuilDoubleValidation());
        
        return "stock/ajustements";
    }
    
    @GetMapping("/ajustements/{id}")
    public String viewAjustement(@PathVariable UUID id, Model model) {
        ajustementStockService.findAjustementById(id).ifPresent(a -> model.addAttribute("ajustement", a));
        return "stock/ajustement-detail";
    }
    
    @GetMapping("/ajustements/add")
    public String addAjustementForm(@RequestParam(required = false) UUID stockId, Model model) {
        model.addAttribute("ajustement", new AjustementStock());
        model.addAttribute("depots", referentielService.findAllDepots());
        model.addAttribute("articles", referentielService.findAllArticles());
        model.addAttribute("motifs", referentielService.findAllMotifsAjustement());
        
        if (stockId != null) {
            stockService.findStockById(stockId).ifPresent(stock -> 
                model.addAttribute("stock", stock)
            );
        }
        
        return "stock/ajustement-form";
    }
    
    @PostMapping("/ajustements/save")
    public String saveAjustement(@ModelAttribute("ajustement") AjustementStock ajustement,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            AjustementStock saved = ajustementStockService.createAjustement(ajustement, user);
            redirectAttributes.addFlashAttribute("success", "Ajustement créé");
            return "redirect:/stock/ajustements/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/stock/ajustements/add";
        }
    }
    
    @PostMapping("/ajustements/{id}/soumettre")
    public String soumettreAjustement(@PathVariable UUID id,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            ajustementStockService.soumettreAjustement(id, user);
            redirectAttributes.addFlashAttribute("success", "Ajustement soumis pour validation");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/ajustements/" + id;
    }
    
    @PostMapping("/ajustements/{id}/approuver-niveau1")
    public String approuverAjustementN1(@PathVariable UUID id,
                                       @RequestParam(required = false) String commentaire,
                                       Authentication auth,
                                       RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            AjustementStock ajustement = ajustementStockService.approuverNiveau1(id, user, commentaire);
            
            if ("APPROUVE_NIVEAU1".equals(ajustement.getStatutCode())) {
                redirectAttributes.addFlashAttribute("warning", "Montant élevé - Double validation requise");
            } else {
                redirectAttributes.addFlashAttribute("success", "Ajustement approuvé");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/ajustements/" + id;
    }
    
    @PostMapping("/ajustements/{id}/approuver-final")
    public String approuverAjustementFinal(@PathVariable UUID id,
                                          @RequestParam(required = false) String commentaire,
                                          Authentication auth,
                                          RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            ajustementStockService.approuverFinal(id, user, commentaire);
            redirectAttributes.addFlashAttribute("success", "Ajustement validé - Peut être exécuté");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/ajustements/" + id;
    }
    
    @PostMapping("/ajustements/{id}/rejeter")
    public String rejeterAjustement(@PathVariable UUID id,
                                   @RequestParam String motif,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            ajustementStockService.rejeterAjustement(id, user, motif);
            redirectAttributes.addFlashAttribute("success", "Ajustement rejeté");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/ajustements/" + id;
    }
    
    @PostMapping("/ajustements/{id}/executer")
    public String executerAjustement(@PathVariable UUID id,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Utilisateur user = getCurrentUser(auth);
            ajustementStockService.executerAjustement(id, user);
            redirectAttributes.addFlashAttribute("success", "Ajustement exécuté - Stock mis à jour");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/ajustements/" + id;
    }
}
