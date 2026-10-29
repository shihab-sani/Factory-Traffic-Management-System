package factory.traffic.management.Services;

import factory.traffic.management.Repositories.JunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final JunctionRepository junctionRepo;
    private final TrafficControlService trafficControl;

    @Scheduled(fixedRateString = "${traffic.scheduler.tick-interval-ms}")
    public void tickAllJunctions() {
        junctionRepo.findAll().forEach(j -> {
            try {
                trafficControl.tick(j);
            } catch (Exception e) {
                log.warn("Tick failed for junction {}: {}", j.getId(), e.getMessage());
            }
        });
    }
}
