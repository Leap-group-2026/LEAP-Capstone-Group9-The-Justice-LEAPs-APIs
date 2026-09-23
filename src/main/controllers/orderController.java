package main.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import main.dto.request.CreateOrderRequest;
import main.entities.OrderEntity;
import main.services.OrderService;

@RestController
@RequestMapping("/api/orders")
public class orderController {
    private static final Logger log = LoggerFactory.getLogger(orderController.class);
    private final OrderService orderService;

    public orderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * PATCH /api/orders/{orderId}/status
     * Update an order's status and record an immutable historical snapshot.
     * 
     * Request body: { "newStatus": "FILLED" }
     * Response: 200 OK with updated order entity
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderEntity> updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestBody StatusUpdateRequest request) {
        
        log.info("Request to update order {} status to {}", orderId, request.getNewStatus());
        
        try {
            OrderEntity updated = orderService.updateOrderStatus(orderId, request.getNewStatus());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Failed to update order {}: {}", orderId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/orders
     * Create a new order with initial snapshot.
     */
    @PostMapping
    public ResponseEntity<OrderEntity> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        log.info("Request to create order: side={}, account={}, instrument={}, quantity={}", 
            request.side(), request.accountId(), request.instrumentId(), request.quantity());
        
        try {
            OrderEntity created = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

/**
 * DTO for PATCH request body.
 * Expects JSON: { "newStatus": "FILLED" }
 */
class StatusUpdateRequest {
    private String newStatus;
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}
