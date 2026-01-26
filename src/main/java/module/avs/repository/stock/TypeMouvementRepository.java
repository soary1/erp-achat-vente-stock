package module.avs.repository.stock;

import module.avs.model.stock.TypeMouvement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeMouvementRepository extends JpaRepository<TypeMouvement, String> {
}
