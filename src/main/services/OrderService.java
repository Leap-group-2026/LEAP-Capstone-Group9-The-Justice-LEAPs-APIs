package main.services;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

import main.entities.OrderEntity;
import main.entities.accountsEntity;
import main.repos.OrdersRepo;
import main.repos.historicalOrdersRepo;
import main.entities.historicalOrdersEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrdersRepo ordersRepo;
    private final historicalOrdersRepo historicalOrdersRepo;
    private final ObjectMapper objectMapper;

    /**
     * Helper method: Serialize an order entity to JSON string.
     * Contains: orderId, side, status, quantity, totalPrice, createdAt, updatedAt, account_id, instrument_id
     * Throws RuntimeException if serialization fails (transaction will rollback).
     */
    private String serializeOrderToJson(OrderEntity order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order {} to JSON", order.getOrderId(), e);
            throw new RuntimeException("Order serialization failed; transaction rolled back", e);
        }
    }

    /**
     * Helper method: Insert an immutable snapshot into historical_orders.
     * This is append-only—the record is never updated or deleted.
     */
    private void captureOrderSnapshot(OrderEntity order) {
        String snapshot = serializeOrderToJson(order);
        
        historicalOrdersEntity history = new historicalOrdersEntity();
        history.setOrderId(order);
        history.setAccount(order.getAccountId());
        history.setOrderInformationJson(snapshot);
        
        historicalOrdersRepo.save(history);
        log.debug("Captured historical snapshot for order {}", order.getOrderId());
    }

    /**
     * Update an order's status and record an immutable snapshot.
     * Both the status update and snapshot insert happen atomically:
     * if one fails, both roll back.
     */
    @Transactional
    public OrderEntity updateOrderStatus(Integer orderId, String newStatus) {
        OrderEntity order = ordersRepo.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Capture the OLD state (before change)
        captureOrderSnapshot(order);
        
        // Mutate the order
        order.setStatus(newStatus);
        OrderEntity updated = ordersRepo.save(order);
        
        // Capture the NEW state (after change)
        captureOrderSnapshot(updated);
        
        log.info("Order {} status changed from PENDING to {}", orderId, newStatus);
        return updated;
    }

    /**
     * Create a new order and capture an initial snapshot.
     * Both the order insert and the snapshot insert happen atomically.
     */
    @Transactional
    public OrderEntity createOrderWithSnapshot(OrderEntity newOrder) {
        // Save the new order first
        OrderEntity saved = ordersRepo.save(newOrder);
        
        // Capture the initial state as a snapshot
        captureOrderSnapshot(saved);
        
        log.info("Order {} created with initial snapshot", saved.getOrderId());
        return saved;
    }

    /**
     * Capture an initial snapshot when an order is first created.
     * Call this from the controller after ordersRepo.save().
     */
    @Transactional
    public void captureInitialOrderSnapshot(Integer orderId) {
        OrderEntity order = ordersRepo.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        captureOrderSnapshot(order);
        log.info("Initial snapshot captured for order {}", orderId);
    }

    public List<historicalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return historicalOrdersRepo.findByOrderId_OrderIdOrderByCreatedAtAsc(orderId);
    }
}
