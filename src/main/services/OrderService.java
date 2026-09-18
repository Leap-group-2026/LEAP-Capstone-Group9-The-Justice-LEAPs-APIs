package main.services;

import org.springframework.stereotype.Service;

import main.repos.OrdersRepo;

@Service
public class OrderService {
    private final OrdersRepo ordersRepo;

    public OrderService(OrdersRepo repo){
        this.ordersRepo = repo;
    }
}
