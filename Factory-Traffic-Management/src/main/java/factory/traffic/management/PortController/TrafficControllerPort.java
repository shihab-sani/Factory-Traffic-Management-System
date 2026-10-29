package factory.traffic.management.PortController;

import factory.traffic.management.Domain.Direction;
import factory.traffic.management.Domain.SignalState;

public interface TrafficControllerPort {
    String sendCommand(String junctionId, Direction direction, SignalState requestedState);
    record AckResult(String commandId, String status, SignalState actualState) {}
}
