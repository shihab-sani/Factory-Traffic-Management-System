package factory.traffic.management.Services;

import factory.traffic.management.Domain.Direction;
import factory.traffic.management.Domain.JunctionMode;
import factory.traffic.management.Domain.VehicleType;
import factory.traffic.management.Dtos.SensorEventRequest;
import factory.traffic.management.Entity.AuditLog;
import factory.traffic.management.Entity.Junction;
import factory.traffic.management.Entity.ProcessedEvent;
import factory.traffic.management.Entity.QueuedVehicle;
import factory.traffic.management.Repositories.AuditLogRepository;
import factory.traffic.management.Repositories.JunctionRepository;
import factory.traffic.management.Repositories.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SensorEventService {
    private final JunctionRepository junctionRepo;
    private final ProcessedEventRepository processedRepo;
    private final AuditLogRepository auditRepo;

    /** Idempotent: same event_id never processed twice. */
    @Transactional
    public void processEvent(SensorEventRequest req) {
        // 1. Idempotency check
        if (processedRepo.existsById(req.eventId())) {
            auditRepo.save(new AuditLog(req.junctionId(), "DUPLICATE_IGNORED",
                    "event_id=" + req.eventId()));
            return;
        }

        // 2. Validate junction exists
        Junction junction = junctionRepo.findById(req.junctionId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown junction: " + req.junctionId()));

        // 3. Handle by type
        switch (req.eventType()) {
            case "VEHICLE_ARRIVED" -> handleArrival(junction, req);
            case "VEHICLE_CLEARED" -> handleClear(junction, req);
            default -> {
                auditRepo.save(new AuditLog(req.junctionId(), "INVALID_EVENT_TYPE",
                        req.eventType()));
                processedRepo.save(new ProcessedEvent(req.eventId(), "REJECTED_INVALID"));
                return;
            }
        }

        processedRepo.save(new ProcessedEvent(req.eventId(), "ACCEPTED"));
        junctionRepo.save(junction); // optimistic locking will protect concurrent writes
        auditRepo.save(new AuditLog(req.junctionId(), req.eventType(),
                "vehicle=" + req.vehicleId() + " dir=" + req.direction()));
    }

    private void handleArrival(Junction junction, SensorEventRequest req) {
        Direction dir = Direction.valueOf(req.direction().toUpperCase());
        VehicleType type = VehicleType.valueOf(req.vehicleType().toUpperCase());

        QueuedVehicle v = new QueuedVehicle();
        v.setJunction(junction);
        v.setDirection(dir);
        v.setVehicleId(req.vehicleId());
        v.setVehicleType(type);
        v.setArrivedAt(req.timestamp() != null ? req.timestamp() : Instant.now());
        v.setSequenceNo(req.sequenceNo() != null ? req.sequenceNo() : 0);
        junction.getQueue().add(v);

        // Emergency: switch mode if not already
        if (type == VehicleType.EMERGENCY && junction.getMode() != JunctionMode.DEGRADED) {
            junction.setMode(JunctionMode.EMERGENCY);
            auditRepo.save(new AuditLog(junction.getId(), "EMERGENCY_DETECTED",
                    "direction=" + dir));
        }
    }

    private void handleClear(Junction junction, SensorEventRequest req) {
        // Best-effort removal; if not present, log but don't fail (delayed/out-of-order tolerated)
        boolean removed = junction.getQueue().removeIf(q ->
                q.getVehicleId().equals(req.vehicleId()));
        if (!removed) {
            auditRepo.save(new AuditLog(junction.getId(), "CLEAR_UNKNOWN_VEHICLE",
                    "vehicle=" + req.vehicleId()));
        }
    }
}
