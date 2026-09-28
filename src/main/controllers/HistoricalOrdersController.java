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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class HistoricalOrdersController {
    
    private final OrderService orderService;
    private final HistoricalOrdersRepo historicalOrdersRepo;
    private final ObjectMapper objectMapper;
    
    public HistoricalOrdersController(OrderService orderService, HistoricalOrdersRepo historicalOrdersRepo, ObjectMapper objectMapper){
        this.orderService = orderService;
        this.historicalOrdersRepo = historicalOrdersRepo;
        this.objectMapper = objectMapper;
    }
    

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
        

        try {
            OrderSnapshot snapshot = objectMapper.readValue(
                entity.getOrderInformationJson(),
                OrderSnapshot.class
            );
            

            response.setStatus(snapshot.getStatus());
            response.setSide(snapshot.getSide());
            response.setQuantity(snapshot.getQuantity());
            response.setTotalPrice(snapshot.getTotalPrice());
            response.setInstrumentId(snapshot.getInstrumentId());
            response.setSnapshotUpdatedAt(snapshot.getUpdatedAt());
            
        } catch (JsonProcessingException e) {
            response.setSnapshotError("Snapshot could not be read");
        }
        
        return response;
    }
}

