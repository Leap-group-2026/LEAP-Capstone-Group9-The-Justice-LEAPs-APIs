package repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import entities.historicalOrdersEntity;

@Repository
public interface historicalOrdersRepo extends JpaRepository<historicalOrdersEntity, Integer>{}