package module.avs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfertDTO {
    private UUID stockId;
    private UUID depotId;
    private String depotName;
    private UUID articleId;
    private String articleLabel;
    private String articleSku;
    private BigDecimal qtyReel;
    private BigDecimal qtyReserve;
    private BigDecimal qtyDisponible;
    private UUID lotId;
    private String lotReference;
}
