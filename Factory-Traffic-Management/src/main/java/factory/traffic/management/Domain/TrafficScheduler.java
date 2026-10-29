package factory.traffic.management.Domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

public class TrafficScheduler {
    public Direction pickDirection(
            Collection<QueuedVehicleRecord> queue,
            Phase lastGreenPhase,
            Duration starvationThreshold,
            Instant now) {

        double nsScore = scoreSide(queue, Direction.NORTH, Direction.SOUTH, starvationThreshold, now);
        double ewScore = scoreSide(queue, Direction.EAST,  Direction.WEST,  starvationThreshold, now);

        // 1. If all roads are empty (score = 0), alternate normally like a standard traffic light
        if (nsScore == 0 && ewScore == 0) {
            if (lastGreenPhase == Phase.NORTH_SOUTH_GREEN) {
                return Direction.EAST; // If North was just green, pick East
            } else {
                return Direction.NORTH; // If East was just green, pick North
            }
        }

        // 2. Hysteresis: keep the current green side unless clearly worse (prevents flapping)
        double threshold = lastGreenPhase != null && lastGreenPhase.isGreen() ? 1.2 : 1.0;

        if (lastGreenPhase == Phase.NORTH_SOUTH_GREEN) {
            return (ewScore > nsScore * threshold) ? Direction.EAST : Direction.NORTH;
        }
        if (lastGreenPhase == Phase.EAST_WEST_GREEN) {
            return (nsScore > ewScore * threshold) ? Direction.NORTH : Direction.EAST;
        }

        return (nsScore >= ewScore) ? Direction.NORTH : Direction.EAST;
    }

    private double scoreSide(Collection<QueuedVehicleRecord> queue,
                             Direction a, Direction b,
                             Duration starvationThreshold, Instant now) {
        double score = 0;
        for (QueuedVehicleRecord v : queue) {
            if (v.direction() != a && v.direction() != b) continue;
            long waitSec = Duration.between(v.arrivedAt(), now).toSeconds();

            // Queue size weight
            score += 10;

            // Vehicle priority weight
            score += v.vehicleType().getPriorityWeight();

            // Waiting time (capped to prevent runaway)
            score += Math.min(200, waitSec * 0.5);

            // Starvation protection: big boost if waiting too long
            if (waitSec > starvationThreshold.toSeconds()) {
                score += 500;
            }
        }
        return score;
    }
}
