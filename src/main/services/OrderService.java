package main.services;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

import main.entities.OrderEntity;
import main.repos.OrdersRepo;
import main.repos.historicalOrdersRepo;
import main.entities.historicalOrdersEntity;

@Service
public class OrderService {
    @Autowired
    private final OrdersRepo ordersRepo;

    @Autowired
    private historicalOrdersRepo historicalOrdersRepo;

    @Transactional
    public void updateOrderStatus(Integer orderId, String newStatus) {
        OrderEntity order = ordersRepo.findById(orderId).orElseThrow();
        order.setStatus(newStatus);
        ordersRepo.save(order);
    }

    public List<historicalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return historicalOrdersRepo.findByOrderId_OrderIdOrderByCreatedAtAsc(orderId);
    }

    public OrderService(OrdersRepo repo){
        this.ordersRepo = repo;
    }
}
