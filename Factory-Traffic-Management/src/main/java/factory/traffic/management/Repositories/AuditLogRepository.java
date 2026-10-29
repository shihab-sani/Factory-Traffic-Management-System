package factory.traffic.management.Repositories;

import factory.traffic.management.Entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByJunctionIdOrderByTimestampDesc(String junctionId);
}
