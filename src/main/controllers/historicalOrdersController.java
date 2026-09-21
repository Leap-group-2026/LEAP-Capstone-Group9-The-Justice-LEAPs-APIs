package main.controllers;

import main.services.historicalOrdersService;
import main.services.OrderService;
import main.entities.historicalOrdersEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class historicalOrdersController {
    private OrderService orderService;
    
    public historicalOrdersController(OrderService orderService){
        this.orderService = orderService;
    }
    
    // GET /api/orders/{orderId}/history
    @GetMapping("/{orderId}/history")
    public List<historicalOrdersEntity> getOrderHistory(@PathVariable Integer orderId) {
        return orderService.getHistoricalOrders(orderId);
    }
}

