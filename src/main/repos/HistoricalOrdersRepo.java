package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.HistoricalOrdersEntity;
import java.util.List;

@Repository
public interface HistoricalOrdersRepo extends JpaRepository<HistoricalOrdersEntity, Integer>{
    List<HistoricalOrdersEntity> findByOrderId_OrderIdOrderByCreatedAtAsc(Integer orderId);
}
