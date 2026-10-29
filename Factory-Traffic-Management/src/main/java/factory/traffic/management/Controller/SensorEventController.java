package factory.traffic.management.Controller;

import factory.traffic.management.Dtos.SensorEventRequest;
import factory.traffic.management.Services.SensorEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor-events")
@RequiredArgsConstructor
public class SensorEventController {
    private final SensorEventService service;

    @PostMapping
    public ResponseEntity<String> post(@RequestBody SensorEventRequest req) {
        try {
            service.processEvent(req);
            return ResponseEntity.ok("ACCEPTED");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
