package module.avs.repository.article;

import module.avs.model.article.ListeTarifaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListeTarifaireRepository extends JpaRepository<ListeTarifaire, UUID> {
    Optional<ListeTarifaire> findByCode(String code);
    List<ListeTarifaire> findByIsActiveTrue();
}
