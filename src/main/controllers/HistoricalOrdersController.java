package main.controllers;

import main.services.OrderService;
import main.entities.HistoricalOrdersEntity;
import main.dto.response.HistoricalOrderResponse;
import main.dto.OrderSnapshot;
import main.repos.HistoricalOrdersRepo;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class HistoricalOrdersController {
    private static final Logger log = LoggerFactory.getLogger(HistoricalOrdersController.class);
    
    private final OrderService orderService;
    private final HistoricalOrdersRepo historicalOrdersRepo;
    private final ObjectMapper objectMapper;
    
    public HistoricalOrdersController(OrderService orderService, HistoricalOrdersRepo historicalOrdersRepo, ObjectMapper objectMapper){
        this.orderService = orderService;
        this.historicalOrdersRepo = historicalOrdersRepo;
        this.objectMapper = objectMapper;
    }
    
    /**
     * GET /orders/{orderId}/history
     * Returns the immutable event log for an order, so disputes can be reconstructed.
     * 
     * Response: Array of events with snapshot at the moment of the event (not current state).
     * Each event includes the status, side, quantity, etc. *as it was* at that time.
     */
    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<HistoricalOrderResponse>> getOrderHistory(
            @PathVariable Integer orderId) {
        
        List<HistoricalOrdersEntity> entities = historicalOrdersRepo.findByOrderId_OrderIdOrderByCreatedAtAsc(orderId);
        
        List<HistoricalOrderResponse> dtos = entities.stream()
            .map(this::mapToHistoricalOrderResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Map a HistoricalOrdersEntity to a HistoricalOrderResponse.
     * Deserializes the stored JSON snapshot and populates response fields from it.
     * This ensures the response contains the status/side/quantity *as it was* at the event time,
     * not the current state of the live order.
     */
    private HistoricalOrderResponse mapToHistoricalOrderResponse(HistoricalOrdersEntity entity) {
        HistoricalOrderResponse response = new HistoricalOrderResponse(
            entity.getHistoricalOrderId(),      // eventId
            entity.getOrderId().getOrderId(),   // orderId
            entity.getAccount().getAccountId(), // accountId
            entity.getOrderInformationJson(),   // snapshot (raw JSON string)
            entity.getCreatedAt()               // occurredAt
        );
        
        // Deserialize the stored snapshot JSON and populate the response with those values
        try {
            OrderSnapshot snapshot = objectMapper.readValue(
                entity.getOrderInformationJson(),
                OrderSnapshot.class
            );
            
            // Populate response from snapshot (these are the values at the time of the event)
            response.setStatus(snapshot.getStatus());
            response.setSide(snapshot.getSide());
            response.setQuantity(snapshot.getQuantity());
            response.setTotalPrice(snapshot.getTotalPrice());
            response.setInstrumentId(snapshot.getInstrumentId());
            response.setSnapshotUpdatedAt(snapshot.getUpdatedAt());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize snapshot JSON for history event {}: {}", 
                entity.getHistoricalOrderId(), e.getMessage());
            // If deserialization fails, response will have null snapshot fields
        }
        
        return response;
    }
}

