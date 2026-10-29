package factory.traffic.management.Controller;

import factory.traffic.management.Repositories.JunctionRepository;
import factory.traffic.management.Services.TrafficControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final JunctionRepository junctionRepo;
    private final TrafficControlService control;

    @PostMapping("/tick/{id}")
    public String tick(@PathVariable String id) {
        var j = junctionRepo.findById(id).orElseThrow();
        boolean changed = control.tick(j);
        return "changed=" + changed + " phase=" + j.getPhase() + " at=" + Instant.now();
    }

    @PostMapping("/set-controller/{id}")
    public String setController(@PathVariable String id, @RequestParam String status) {
        var j = junctionRepo.findById(id).orElseThrow();
        j.setControllerStatus(status);
        junctionRepo.save(j);
        return "controller=" + status;
    }
}
