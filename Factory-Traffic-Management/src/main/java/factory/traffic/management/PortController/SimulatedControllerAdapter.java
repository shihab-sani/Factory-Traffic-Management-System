package factory.traffic.management.PortController;

import factory.traffic.management.Domain.Direction;
import factory.traffic.management.Domain.SignalState;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedControllerAdapter implements TrafficControllerPort {
    @Override
    public String sendCommand(String junctionId, Direction direction, SignalState requestedState) {
        return "cmd-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
