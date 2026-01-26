package module.avs.model.referentiel;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "type_taxe")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TypeTaxe {
    
    @Id
    @Column(length = 20)
    private String code;
    
    @Column(precision = 5, scale = 4, nullable = false)
    private BigDecimal rate;
    
    @Column(length = 100, nullable = false)
    private String label;
}
