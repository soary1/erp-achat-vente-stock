package module.avs.dto;

import lombok.*;
import module.avs.model.stock.Stock;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO pour afficher le stock avec sa valorisation calculée
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockValorisationDTO {
    
    // Identifiant du stock
    private UUID stockId;
    
    // Informations article
    private UUID articleId;
    private String articleLabel;
    private String articleSku;
    
    // Informations famille et méthode valorisation
    private String familleName;
    private String methodeValorisationCode;
    private String methodeValorisationLabel;
    
    // Informations dépôt/emplacement
    private UUID depotId;
    private String depotName;
    private String emplacementCode;
    
    // Informations lot
    private UUID lotId;
    private String lotNumero;
    
    // Quantités
    private BigDecimal qtyReel;
    private BigDecimal qtyReserve;
    private BigDecimal qtyDisponible;
    
    // Valorisation
    private BigDecimal prixUnitaire;
    private BigDecimal valeurStock;
    
    /**
     * Construit le DTO depuis une entité Stock avec les valeurs calculées
     */
    public static StockValorisationDTO fromStock(Stock stock, BigDecimal prixUnitaire) {
        BigDecimal valeurStock = BigDecimal.ZERO;
        if (prixUnitaire != null && stock.getQtyReel() != null) {
            valeurStock = prixUnitaire.multiply(stock.getQtyReel());
        }
        
        return StockValorisationDTO.builder()
            .stockId(stock.getId())
            .articleId(stock.getArticle() != null ? stock.getArticle().getId() : null)
            .articleLabel(stock.getArticle() != null ? stock.getArticle().getLabel() : null)
            .articleSku(stock.getArticle() != null ? stock.getArticle().getSku() : null)
            .familleName(stock.getArticle() != null && stock.getArticle().getFamille() != null 
                ? stock.getArticle().getFamille().getName() : null)
            .methodeValorisationCode(stock.getArticle() != null && stock.getArticle().getFamille() != null 
                && stock.getArticle().getFamille().getMethodeValorisation() != null
                ? stock.getArticle().getFamille().getMethodeValorisation().getCode() : null)
            .methodeValorisationLabel(stock.getArticle() != null && stock.getArticle().getFamille() != null 
                && stock.getArticle().getFamille().getMethodeValorisation() != null
                ? stock.getArticle().getFamille().getMethodeValorisation().getLabel() : null)
            .depotId(stock.getDepot() != null ? stock.getDepot().getId() : null)
            .depotName(stock.getDepot() != null ? stock.getDepot().getName() : null)
            .emplacementCode(stock.getEmplacement() != null ? stock.getEmplacement().getCode() : null)
            .lotId(stock.getLot() != null ? stock.getLot().getId() : null)
            .lotNumero(stock.getLot() != null ? stock.getLot().getNumeroLot() : null)
            .qtyReel(stock.getQtyReel())
            .qtyReserve(stock.getQtyReserve())
            .qtyDisponible(stock.getQtyDisponible())
            .prixUnitaire(prixUnitaire != null ? prixUnitaire : BigDecimal.ZERO)
            .valeurStock(valeurStock)
            .build();
    }
}
