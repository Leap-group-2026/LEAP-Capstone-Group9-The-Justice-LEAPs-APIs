package main.services;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import main.dto.request.CreateOrderRequest;
import main.entities.OrderEntity;
import main.entities.accountsEntity;
import main.entities.instrumentEntity;
import main.entities.historicalOrdersEntity;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.instrumentRepo;
import main.repos.historicalOrdersRepo;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrdersRepo ordersRepo;
    private final AccountsRepo accountsRepo;
    private final instrumentRepo instrumentRepo;
    private final historicalOrdersRepo historicalOrdersRepo;
    private final ObjectMapper objectMapper;

    public OrderService(OrdersRepo ordersRepo, AccountsRepo accountsRepo, instrumentRepo instrumentRepo,
                       historicalOrdersRepo historicalOrdersRepo, ObjectMapper objectMapper) {
        this.ordersRepo = ordersRepo;
        this.accountsRepo = accountsRepo;
        this.instrumentRepo = instrumentRepo;
        this.historicalOrdersRepo = historicalOrdersRepo;
        this.objectMapper = objectMapper;
    }

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
     * Create a new order from a CreateOrderRequest.
     * Validates that the account and instrument exist.
     * Captures an initial snapshot of the new order.
     */
    @Transactional
    public OrderEntity createOrder(CreateOrderRequest request) {
        accountsEntity account = accountsRepo.findById(request.accountId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        instrumentEntity instrument = instrumentRepo.findById(request.instrumentId())
            .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));

        OrderEntity order = new OrderEntity(
            request.side(),
            account,
            instrument,
            request.quantity(),
            BigDecimal.valueOf(420.69)
        );

        return createOrderWithSnapshot(order);
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
        
        log.info("Order {} status changed to {}", orderId, newStatus);
        return updated;
    }

    /**
     * Retrieve all historical snapshots for an order in chronological order.
     */
    public List<historicalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return historicalOrdersRepo.findByOrderId_OrderIdOrderByCreatedAtAsc(orderId);
    }
}
