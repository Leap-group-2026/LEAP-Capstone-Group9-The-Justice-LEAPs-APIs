package main.controllers;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import main.dto.request.CreateOrderRequest;
import main.dto.response.OrderSubmissionResponse;
import main.services.OrderService;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderSubmissionResponse createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
