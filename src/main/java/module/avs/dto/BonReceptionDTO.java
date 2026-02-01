package module.avs.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class BonReceptionDTO {
    private UUID id;
    private UUID commandeAchatId;
    private UUID depotId;
    private OffsetDateTime dateReception;
    private List<LigneReceptionDTO> lignes = new ArrayList<>();
    
    @Data
    public static class LigneReceptionDTO {
        private UUID ligneCommandeId;
        private UUID articleId;
        private Double qtyReceived;
        private Double unitCost;
        private String numeroLot;
        private String datePeremption;
        private UUID emplacementId;
    }
}
