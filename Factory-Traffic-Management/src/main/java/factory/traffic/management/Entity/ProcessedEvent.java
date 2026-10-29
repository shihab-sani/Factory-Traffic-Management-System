package factory.traffic.management.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_event")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedEvent {
    @Id
    private String eventId; // Removed @GeneratedValue because it's a String

    private Instant processedAt = Instant.now();
    private String outcome;

    // This constructor actually assigns the values properly!
    public ProcessedEvent(String eventId, String outcome) {
        this.eventId = eventId;
        this.outcome = outcome;
    }
}
