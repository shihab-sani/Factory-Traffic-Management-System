package factory.traffic.management.Controller;

import factory.traffic.management.Domain.Direction;
import factory.traffic.management.Domain.JunctionMode;
import factory.traffic.management.Dtos.CommandRequest;
import factory.traffic.management.Dtos.JunctionStatusResponse;
import factory.traffic.management.Entity.AuditLog;
import factory.traffic.management.Entity.Junction;
import factory.traffic.management.Repositories.AuditLogRepository;
import factory.traffic.management.Repositories.JunctionRepository;
import factory.traffic.management.Services.TrafficControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/junctions")
@RequiredArgsConstructor
public class JunctionController {
    private final JunctionRepository junctionRepo;
    private final AuditLogRepository auditRepo;
    private final TrafficControlService control;

    @GetMapping
    public List<Map<String, Object>> list() {
        return junctionRepo.findAll().stream()
                .map(j -> Map.<String, Object>of(
                        "id", j.getId(),
                        "name", j.getName(),
                        "mode", j.getMode(),
                        "phase", j.getPhase()))
                .toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable String id) {
        var j = junctionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Junction not found: " + id));
        return Map.of("id", j.getId(), "name", j.getName(), "mode", j.getMode(),
                "phase", j.getPhase());
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        String id = body.getOrDefault("id", "A");
        Junction j = new Junction();
        j.setId(id);
        j.setName(body.getOrDefault("name", "Junction " + id));
        junctionRepo.save(j);
        return Map.of("id", id, "created", true);
    }

    @GetMapping("/{id}/status")
    public JunctionStatusResponse status(@PathVariable String id) {
        var j = junctionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Junction not found: " + id));

        Map<String, Integer> queues = new LinkedHashMap<>();
        for (Direction d : Direction.values()) {
            queues.put(d.name(), (int) j.getQueue().stream()
                    .filter(q -> q.getDirection() == d).count());
        }

        Map<String, String> desired = j.getDesiredMap().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().name()));
        Map<String, String> actual = j.getActualMap().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().name()));

        return new JunctionStatusResponse(
                j.getId(), j.getMode().name(), j.getPhase().name(), j.getControllerStatus(),
                desired, actual, queues,
                j.getMode() == JunctionMode.EMERGENCY,
                j.getPendingCommandId());
    }

    @PostMapping("/{id}/commands")
    public Map<String, Object> command(@PathVariable String id, @RequestBody CommandRequest req) {
        switch (req.command()) {
            case "MANUAL_GREEN_REQUEST" -> control.requestManualGreen(id,
                    Direction.valueOf(req.direction().toUpperCase()));
            case "RETURN_TO_AUTOMATIC" -> control.returnToAutomatic(id);
            default -> throw new IllegalArgumentException("Unknown command: " + req.command());
        }
        return Map.of("accepted", true, "command", req.command());
    }

    @GetMapping("/{id}/history")
    public List<AuditLog> history(@PathVariable String id) {
        return auditRepo.findTop50ByJunctionIdOrderByTimestampDesc(id);
    }
}
