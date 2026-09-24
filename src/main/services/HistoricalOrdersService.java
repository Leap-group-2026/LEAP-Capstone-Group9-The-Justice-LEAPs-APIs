package main.services;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import main.entities.OrderEntity;
import main.repos.HistoricalOrdersRepo;
import main.repos.OrdersRepo;
import main.entities.HistoricalOrdersEntity;
import main.dto.OrderSnapshot;
import java.util.List;

@Service
public class HistoricalOrdersService {
    private static final Logger log = LoggerFactory.getLogger(HistoricalOrdersService.class);
    private final HistoricalOrdersRepo repo;
    private final ObjectMapper objectMapper;
    private final OrdersRepo ordersRepo;

    public HistoricalOrdersService(HistoricalOrdersRepo repo, ObjectMapper objectMapper, OrdersRepo ordersRepo) {
        this.repo = repo;
        this.objectMapper = objectMapper;
        this.ordersRepo = ordersRepo;
    }

    /**
     * Capture an immutable snapshot of the order's current state.
     * Stores ONLY scalar values and IDs, not entity references.
     */
    public void captureOrderSnapshot(OrderEntity order) {
        log.info("captureOrderSnapshot called: orderId={} status={} updatedAt={}", 
            order.getOrderId(), order.getStatus(), order.getUpdatedAt());
            
        // Use scalar ID values (always populated by MyBatis from FK columns)
        Integer instrumentId = order.getInstrumentIdValue();
        Integer accountId = order.getAccountIdValue();
        
        // Build a snapshot containing only scalars and IDs, not entity objects
        OrderSnapshot snapshot = new OrderSnapshot(
            order.getStatus(),                              // status at this moment
            order.getSide(),
            order.getQuantity(),
            order.getTotalPrice(),
            instrumentId,                                   // ID from scalar field
            accountId,                                      // ID from scalar field
            order.getUpdatedAt()
        );
        
        String jsonSnapshot = serializeSnapshotToJson(snapshot);
        log.info("Snapshot JSON: {}", jsonSnapshot);
        
        // Insert using MyBatis (use safely extracted IDs)
        log.info("Inserting snapshot: orderId={} accountId={} createdAt={}", 
            order.getOrderId(), accountId, order.getCreatedAt());
        repo.insert(
            order.getOrderId(),
            accountId,
            jsonSnapshot,
            order.getCreatedAt()
        );
        log.info("Snapshot inserted successfully");
    }

    /**
     * Retrieve all historical snapshots for an order in chronological order.
     */
    public List<HistoricalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return repo.findByOrderId_OrderIdOrderByCreatedAtAsc(orderId);
    }

    /**
     * Serialize an OrderSnapshot (not the full entity) to JSON string for storage.
     * Throws RuntimeException if serialization fails.
     */
    private String serializeSnapshotToJson(OrderSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order snapshot to JSON: {}", e.getMessage());
            throw new RuntimeException("Order snapshot serialization failed", e);
        }
    }

    public String serializeOrderToJson(OrderEntity entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.error("Error serializing historical order entity", e);
            return null;
        }
    }

    public HistoricalOrdersEntity saveHistoricalOrder(HistoricalOrdersEntity entity){
        repo.insert(entity.getOrderId(), entity.getAccountId(), entity.getOrderInformationJson(), entity.getCreatedAt());
        return entity;
    }
}