package factory.traffic.management.Repositories;

import factory.traffic.management.Entity.Junction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JunctionRepository extends JpaRepository<Junction, String> {
}
