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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.stream.Collectors;

@Tag(name = "Order history")  // description and display order: OpenApiConfig
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
    

    @Operation(summary = "Get an order's history",
        description = "Returns every recorded event for the order, oldest first. Each event carries the order's state "
            + "as it was at that moment (not its current state). If a stored snapshot can't be read, the event is still "
            + "returned with snapshot_error set and the snapshot fields null.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Events in chronological order; an empty array for an order with no history or that doesn't exist")
    })
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

