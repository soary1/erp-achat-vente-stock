package module.avs.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoLocationDTO {
    private UUID id;
    private String code;
    private String name;
    private String type; // SITE, DEPOT, CLIENT, FOURNISSEUR
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String popupContent;
    private String iconColor; // pour différencier les types sur la carte
}
