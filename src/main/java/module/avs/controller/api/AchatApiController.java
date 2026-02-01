package module.avs.controller.api;

import lombok.RequiredArgsConstructor;
import module.avs.model.achat.CommandeAchat;
import module.avs.model.achat.LigneCommandeAchat;
import module.avs.service.AchatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/achats")
@RequiredArgsConstructor
public class AchatApiController {

    private final AchatService achatService;

    /**
     * Récupère les lignes d'une commande d'achat pour le formulaire de réception
     */
    @GetMapping("/commandes/{id}/lignes")
    public ResponseEntity<List<Map<String, Object>>> getCommandeLignes(@PathVariable UUID id) {
        Optional<CommandeAchat> commandeOpt = achatService.findCommandeAchatById(id);
        
        if (commandeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CommandeAchat commande = commandeOpt.get();
        List<Map<String, Object>> lignes = new ArrayList<>();
        
        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Map<String, Object> ligneMap = new HashMap<>();
            ligneMap.put("id", ligne.getId());
            ligneMap.put("qtyOrdered", ligne.getQtyOrdered());
            ligneMap.put("qtyReceived", 0); // TODO: calculer depuis les réceptions existantes
            ligneMap.put("unitPrice", ligne.getUnitPrice());
            
            // Article info
            Map<String, Object> articleMap = new HashMap<>();
            if (ligne.getArticle() != null) {
                articleMap.put("id", ligne.getArticle().getId());
                articleMap.put("sku", ligne.getArticle().getSku());
                articleMap.put("label", ligne.getArticle().getLabel());
            }
            ligneMap.put("article", articleMap);
            
            lignes.add(ligneMap);
        }
        
        return ResponseEntity.ok(lignes);
    }
    
    /**
     * Récupère les détails d'une commande d'achat
     */
    @GetMapping("/commandes/{id}")
    public ResponseEntity<Map<String, Object>> getCommande(@PathVariable UUID id) {
        Optional<CommandeAchat> commandeOpt = achatService.findCommandeAchatById(id);
        
        if (commandeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CommandeAchat commande = commandeOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("id", commande.getId());
        result.put("numero", commande.getNumero());
        result.put("statut", commande.getStatutCode());
        result.put("totalHt", commande.getTotalHT());
        result.put("totalTtc", commande.getTotalTTC());
        
        if (commande.getFournisseur() != null) {
            Map<String, Object> fournisseur = new HashMap<>();
            fournisseur.put("id", commande.getFournisseur().getId());
            fournisseur.put("name", commande.getFournisseur().getName());
            result.put("fournisseur", fournisseur);
        }
        
        return ResponseEntity.ok(result);
    }
}
