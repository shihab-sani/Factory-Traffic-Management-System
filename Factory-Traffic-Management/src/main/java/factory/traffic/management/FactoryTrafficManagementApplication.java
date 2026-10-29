package factory.traffic.management;

import factory.traffic.management.Entity.Junction;
import factory.traffic.management.Repositories.JunctionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FactoryTrafficManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(FactoryTrafficManagementApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDatabase(JunctionRepository junctionRepository) {
        return args -> {
            if (junctionRepository.findById("A").isEmpty()) {
                Junction junctionA = new Junction();
                junctionA.setId("A");
                junctionA.setName("Junction A");
                // Set default modes/states if your entity requires them to not be null
                junctionRepository.save(junctionA);
                System.out.println("✅ SEEDED: Junction A created in database!");
            }
        };
    }
}
