package factory.traffic.management.Domain;

import java.time.Instant;

public record QueuedVehicleRecord(
        String vehicleId,
        Direction direction,
        VehicleType vehicleType,
        Instant arrivedAt,
        long sequenceNo
) {}
