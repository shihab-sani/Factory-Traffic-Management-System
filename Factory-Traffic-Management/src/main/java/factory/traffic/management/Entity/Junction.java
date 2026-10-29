package factory.traffic.management.Entity;

import factory.traffic.management.Domain.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "junction")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Junction {
    @Id
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private JunctionMode mode = JunctionMode.AUTOMATIC;

    @Enumerated(EnumType.STRING)
    private Phase phase = Phase.ALL_RED;

    private Instant phaseStartedAt = Instant.now();

    // Desired (what we asked for) vs Actual (what controller confirmed)
    @Enumerated(EnumType.STRING) private SignalState desiredNorth = SignalState.RED;
    @Enumerated(EnumType.STRING) private SignalState desiredSouth = SignalState.RED;
    @Enumerated(EnumType.STRING) private SignalState desiredEast  = SignalState.RED;
    @Enumerated(EnumType.STRING) private SignalState desiredWest  = SignalState.RED;

    @Enumerated(EnumType.STRING) private SignalState actualNorth = SignalState.RED;
    @Enumerated(EnumType.STRING) private SignalState actualSouth = SignalState.RED;
    @Enumerated(EnumType.STRING) private SignalState actualEast  = SignalState.RED;
    @Enumerated(EnumType.STRING) private SignalState actualWest  = SignalState.RED;

    private String controllerStatus = "ONLINE"; // ONLINE / OFFLINE / DEGRADED / UNKNOWN

    // Pending command tracking
    private String pendingCommandId;
    @Enumerated(EnumType.STRING)
    private Direction pendingCommandDirection;
    private Instant pendingCommandExpiresAt;

    // Manual override tracking
    private Instant manualOverrideExpiresAt;

    @OneToMany(mappedBy = "junction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QueuedVehicle> queue = new ArrayList<>();

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private Phase lastGreenPhase = Phase.NORTH_SOUTH_GREEN;

    // ---- Domain behavior ----

    public SignalState getDesiredFor(Direction d) {
        return switch (d) {
            case NORTH -> desiredNorth; case SOUTH -> desiredSouth;
            case EAST -> desiredEast;   case WEST -> desiredWest;
        };
    }

    public SignalState getActualFor(Direction d) {
        return switch (d) {
            case NORTH -> actualNorth; case SOUTH -> actualSouth;
            case EAST -> actualEast;   case WEST -> actualWest;
        };
    }

    public void setDesiredFromPhase(Phase p) {
        var s = p.signals();
        desiredNorth = s.get(Direction.NORTH);
        desiredSouth = s.get(Direction.SOUTH);
        desiredEast  = s.get(Direction.EAST);
        desiredWest  = s.get(Direction.WEST);
    }

    public void setActualFromPhase(Phase p) {
        var s = p.signals();
        actualNorth = s.get(Direction.NORTH);
        actualSouth = s.get(Direction.SOUTH);
        actualEast  = s.get(Direction.EAST);
        actualWest  = s.get(Direction.WEST);
    }

    public void clearQueueFor(Direction d) {
        queue.removeIf(q -> q.getDirection() == d);
    }

    public void clearQueueForPhase(Phase p) {
        if (p == Phase.NORTH_SOUTH_GREEN || p == Phase.NORTH_SOUTH_YELLOW) {
            clearQueueFor(Direction.NORTH);
            clearQueueFor(Direction.SOUTH);
        } else if (p == Phase.EAST_WEST_GREEN || p == Phase.EAST_WEST_YELLOW) {
            clearQueueFor(Direction.EAST);
            clearQueueFor(Direction.WEST);
        }
    }

    public boolean hasEmergencyInQueue() {
        return queue.stream().anyMatch(q -> q.getVehicleType() == VehicleType.EMERGENCY);
    }

    public Optional<Direction> emergencyDirection() {
        return queue.stream()
                .filter(q -> q.getVehicleType() == VehicleType.EMERGENCY)
                .map(QueuedVehicle::getDirection)
                .findFirst();
    }

    public Map<Direction, SignalState> getDesiredMap() {
        return Map.of(Direction.NORTH, desiredNorth, Direction.SOUTH, desiredSouth,
                Direction.EAST, desiredEast, Direction.WEST, desiredWest);
    }
    public Map<Direction, SignalState> getActualMap() {
        return Map.of(Direction.NORTH, actualNorth, Direction.SOUTH, actualSouth,
                Direction.EAST, actualEast, Direction.WEST, actualWest);
    }
}
