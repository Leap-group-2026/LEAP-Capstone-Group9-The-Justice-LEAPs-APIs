package main.repos;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import main.entities.OrderEntity;

@Repository
public interface OrdersRepo extends JpaRepository<OrderEntity, Integer> {
}