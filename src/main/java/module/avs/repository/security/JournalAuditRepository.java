package module.avs.repository.security;

import module.avs.model.security.JournalAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, UUID> {
    List<JournalAudit> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, UUID entityId);
    Page<JournalAudit> findByEntityNameOrderByCreatedAtDesc(String entityName, Pageable pageable);
    Page<JournalAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
