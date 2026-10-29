package factory.traffic.management.Controller;

import factory.traffic.management.Dtos.ControllerEventRequest;
import factory.traffic.management.Services.TrafficControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/controller-events")
@RequiredArgsConstructor
public class ControllerEventController {
    private final TrafficControlService control;

    @PostMapping
    public String handle(@RequestBody ControllerEventRequest req) {
        control.handleControllerEvent(req.commandId(), req.status(), req.actualState(), req.junctionId());
        return "OK";
    }
}
