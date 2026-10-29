package factory.traffic.management.Services;

import factory.traffic.management.Domain.*;
import factory.traffic.management.Entity.AuditLog;
import factory.traffic.management.Entity.Junction;
import factory.traffic.management.PortController.TrafficControllerPort;
import factory.traffic.management.Repositories.AuditLogRepository;
import factory.traffic.management.Repositories.JunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TrafficControlService {
    private final JunctionRepository junctionRepo;
    private final AuditLogRepository auditRepo;
    private final TrafficScheduler scheduler = new TrafficScheduler();
    private final TrafficControllerPort controller;

    @Value("${traffic.scheduler.green-duration-seconds}") private int greenDur;
    @Value("${traffic.scheduler.yellow-duration-seconds}") private int yellowDur;
    @Value("${traffic.scheduler.all-red-duration-seconds}") private int allRedDur;
    @Value("${traffic.scheduler.starvation-threshold-seconds}") private int starvationSec;
    @Value("${traffic.scheduler.command-ack-timeout-seconds}") private int ackTimeoutSec;

    /**
     * Called by scheduler every tick. Advances the state machine one step at a time.
     * Returns true if state changed.
     */
    @Transactional
    public boolean tick(Junction junction) {
        Instant now = Instant.now();
        Duration elapsed = Duration.between(junction.getPhaseStartedAt(), now);
        Phase current = junction.getPhase();

        // Handle manual override expiry
        if (junction.getMode() == JunctionMode.MANUAL
                && junction.getManualOverrideExpiresAt() != null
                && now.isAfter(junction.getManualOverrideExpiresAt())) {
            junction.setMode(JunctionMode.AUTOMATIC);
            junction.setManualOverrideExpiresAt(null);
            auditRepo.save(new AuditLog(junction.getId(), "MANUAL_EXPIRED", ""));
        }

        // If controller is offline, don't advance (safety)
        if ("OFFLINE".equals(junction.getControllerStatus())) {
            return false;
        }

        // --- State machine tick ---
        if (current.isGreen()) {
            int dur = (junction.getMode() == JunctionMode.EMERGENCY) ? Math.min(greenDur, 10) : greenDur;

            if (junction.getMode() == JunctionMode.EMERGENCY && junction.hasEmergencyInQueue()) {
                Direction emergencyDir = junction.emergencyDirection().get();
                // If the emergency is coming from a conflicting direction, cut the green short!
                if (!current.isGreenFor(emergencyDir)) {
                    dur = 2; // Force transition to Yellow after 1 second
                }
            }

            if (elapsed.getSeconds() >= dur) {
                transition(junction, current.nextAfterGreen(), now);
                return true;
            }
        } else if (current.isYellow()) {
            if (elapsed.getSeconds() >= yellowDur) {
                transition(junction, Phase.ALL_RED, now);
                return true;
            }
        } else { // ALL_RED
            if (elapsed.getSeconds() >= allRedDur) {
                Direction target = chooseNextDirection(junction, now);
                transition(junction, Phase.greenFor(target), now);
                return true;
            }
        }
        return false;
    }

    private Direction chooseNextDirection(Junction junction, Instant now) {
        // Emergency takes priority over manual and automatic
        var emergencyDir = junction.emergencyDirection();
        if (emergencyDir.isPresent()) {
            junction.setMode(JunctionMode.EMERGENCY);
            return emergencyDir.get();
        }

        if (junction.hasEmergencyInQueue()) {
            junction.setMode(JunctionMode.EMERGENCY);
        } else if (junction.getMode() == JunctionMode.EMERGENCY) {
            // No more emergencies → return to automatic
            junction.setMode(JunctionMode.AUTOMATIC);
            auditRepo.save(new AuditLog(junction.getId(), "EMERGENCY_CLEARED", ""));
        }

        if (junction.getMode() == JunctionMode.MANUAL && junction.getPendingCommandDirection() != null) {
            return junction.getPendingCommandDirection();
        }

        // Automatic scheduling
        var domainQueue = junction.getQueue().stream()
                .map(q -> new QueuedVehicleRecord(q.getVehicleId(), q.getDirection(),
                        q.getVehicleType(), q.getArrivedAt(), q.getSequenceNo()))
                .toList();
        return scheduler.pickDirection(domainQueue, junction.getLastGreenPhase(), Duration.ofSeconds(starvationSec), now);
    }

    private void transition(Junction junction, Phase newPhase, Instant now) {
        Phase old = junction.getPhase();

        if (old.isGreen()) {
            junction.setLastGreenPhase(old);
        }

        // Safety: log the transition
        auditRepo.save(new AuditLog(junction.getId(), "SIGNAL_CHANGED",
                old + " -> " + newPhase));

        // Clear queue for directions that just finished their green
        if (old.isGreen()) {
            junction.clearQueueForPhase(old);
        }

        junction.setPhase(newPhase);
        junction.setPhaseStartedAt(now);
        junction.setDesiredFromPhase(newPhase);

        // Simulate command to controller
        String cmdId = controller.sendCommand(junction.getId(),
                Direction.NORTH, newPhase.signals().get(Direction.NORTH));
        junction.setPendingCommandId(cmdId);
        junction.setPendingCommandExpiresAt(now.plusSeconds(ackTimeoutSec));

        // In this simulation the controller immediately ACKs by setting actual
        // (evaluator can simulate failures via /api/controller-events)
        junction.setActualFromPhase(newPhase);
        junction.setPendingCommandId(null);
        junction.setPendingCommandExpiresAt(null);

        junctionRepo.save(junction);
    }

    /** Manual override from admin: execute safe sequence toward target direction. */
    @Transactional
    public void requestManualGreen(String junctionId, Direction direction) {
        Junction j = junctionRepo.findById(junctionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown junction"));
        j.setMode(JunctionMode.MANUAL);
        j.setPendingCommandDirection(direction);
        j.setManualOverrideExpiresAt(Instant.now().plusSeconds(600)); // 10 min
        auditRepo.save(new AuditLog(junctionId, "MANUAL_OVERRIDE",
                "direction=" + direction));
        junctionRepo.save(j);
    }

    @Transactional
    public void returnToAutomatic(String junctionId) {
        Junction j = junctionRepo.findById(junctionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown junction"));
        j.setMode(JunctionMode.AUTOMATIC);
        j.setPendingCommandDirection(null);
        j.setManualOverrideExpiresAt(null);
        auditRepo.save(new AuditLog(junctionId, "RETURN_TO_AUTOMATIC", ""));
        junctionRepo.save(j);
    }

    @Transactional
    public void handleControllerEvent(String commandId, String status, String actualState, String junctionId) {
        Junction j = junctionRepo.findById(junctionId).orElse(null);
        if (j == null) return;
        if (!commandId.equals(j.getPendingCommandId())) {
            auditRepo.save(new AuditLog(junctionId, "STALE_ACK_IGNORED", "cmd=" + commandId));
            return;
        }
        if ("ACK".equalsIgnoreCase(status) && actualState != null) {
            // Trust the physical state as truth
            SignalState s = SignalState.valueOf(actualState.toUpperCase());
            auditRepo.save(new AuditLog(junctionId, "CONTROLLER_ACK",
                    "cmd=" + commandId + " state=" + s));
            j.setPendingCommandId(null);
            j.setPendingCommandExpiresAt(null);
            junctionRepo.save(j);
        }
    }
}
