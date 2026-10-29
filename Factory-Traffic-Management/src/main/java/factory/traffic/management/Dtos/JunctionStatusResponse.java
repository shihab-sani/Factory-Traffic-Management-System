package factory.traffic.management.Dtos;

import java.util.Map;

public record JunctionStatusResponse(
        String junctionId,
        String mode,
        String phase,
        String controllerStatus,
        Map<String, String> desiredSignals,
        Map<String, String> actualSignals,
        Map<String, Integer> queues,
        boolean emergencyActive,
        String pendingCommandId
) {}
