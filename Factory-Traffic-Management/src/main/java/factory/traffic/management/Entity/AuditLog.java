package factory.traffic.management.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String junctionId;
    private String eventType;
    private String direction;
    private String previousState;
    private String newState;
    private String commandId;

    @Column(length = 1000)
    private String details;

    private Instant timestamp = Instant.now();

    public AuditLog(String junctionId, String eventType, String details) {
        this.junctionId = junctionId;
        this.eventType = eventType;
        this.details = details;
    }
}
