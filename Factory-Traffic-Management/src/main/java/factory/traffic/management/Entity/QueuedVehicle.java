package factory.traffic.management.Entity;

import factory.traffic.management.Domain.Direction;
import factory.traffic.management.Domain.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "queued_vehicle")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueuedVehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "junction_id")
    private Junction junction;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    private String vehicleId;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    private Instant arrivedAt;
    private long sequenceNo;

    public QueuedVehicle(String vehicleId, Direction direction, VehicleType vehicleType, Instant arrivedAt, long sequenceNo) {
    }
}
