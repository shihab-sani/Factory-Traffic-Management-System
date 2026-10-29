package factory.traffic.management.Dtos;

public record CommandRequest(
        String command,
        String direction
) {}
