package module.avs.repository.stock;

import module.avs.model.stock.LigneBonReception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneBonReceptionRepository extends JpaRepository<LigneBonReception, UUID> {
    List<LigneBonReception> findByBonReceptionId(UUID bonReceptionId);
    
    @Query("SELECT COALESCE(SUM(lbr.qtyReceived), 0) FROM LigneBonReception lbr " +
           "JOIN lbr.bonReception br " +
           "WHERE br.commandeAchat.id = :commandeId " +
           "AND lbr.article.id = :articleId " +
           "AND br.statutCode = 'VALIDE'")
    BigDecimal sumQtyReceivedForCommandeAndArticle(@Param("commandeId") UUID commandeId, @Param("articleId") UUID articleId);
}
