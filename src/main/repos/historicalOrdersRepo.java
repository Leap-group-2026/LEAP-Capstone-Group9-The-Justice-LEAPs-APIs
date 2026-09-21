package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.historicalOrdersEntity;
import java.util.List;

@Repository
public interface historicalOrdersRepo extends JpaRepository<historicalOrdersEntity, Integer>{
    List<historicalOrdersEntity> findByOrderId_OrderIdOrderByCreatedAtAsc(Integer orderId);
}