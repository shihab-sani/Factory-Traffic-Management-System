package factory.traffic.management.Dtos;

import java.time.Instant;

public record SensorEventRequest(
    String eventId,
    String junctionId,
    String direction,
    String eventType,
    String vehicleId,
    String vehicleType,
    Long sequenceNo,
    Instant timestamp
){}
