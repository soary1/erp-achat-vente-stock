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
public class LigneTransfertDTO {
    private UUID articleId;
    private UUID lotId;
    private BigDecimal quantite;
}
