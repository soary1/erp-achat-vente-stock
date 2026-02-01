package module.avs.repository.referentiel;

import module.avs.model.referentiel.MotifSortie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MotifSortieRepository extends JpaRepository<MotifSortie, String> {
    List<MotifSortie> findByType(String type);
}
