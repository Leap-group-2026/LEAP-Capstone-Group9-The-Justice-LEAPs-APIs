package main.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import main.services.OrderService;
import main.entities.OrderEntity;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class orderController {
    private final OrderService orderService;

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
