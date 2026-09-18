package main.controllers;

import org.springframework.web.bind.annotation.*;

import main.services.OrderService;

@RestController
@RequestMapping("/orders")
public class orderController {
    private final OrderService orderService;

    public orderController(OrderService orderService) {
        this.orderService = orderService;
    }
}
