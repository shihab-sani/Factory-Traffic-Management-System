package factory.traffic.management.Dtos;

public record ControllerEventRequest(
        String commandId,
        String junctionId,
        String status,
        String actualState
) {}
