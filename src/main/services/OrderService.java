package main.services;

import org.springframework.stereotype.Service;

import main.dto.request.CreateOrderRequest;
import main.entities.OrderEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.InstrumentRepo;
import main.repos.HistoricalOrdersRepo;
import main.entities.HistoricalOrdersEntity;
import main.services.validation.BuyOrderValidator;
import main.services.calculation.OrderPriceCalculator;
import main.services.resolver.AccountResolver;
import main.services.resolver.InstrumentResolver;
import java.math.BigDecimal;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.dto.OrderSnapshot;
import java.util.List;


@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrdersRepo ordersRepo;
    private final HistoricalOrdersRepo historicalOrdersRepo;
    private final BuyOrderValidator buyOrderValidator;
    private final OrderPriceCalculator priceCalculator;
    private final AccountResolver accountResolver;
    private final InstrumentResolver instrumentResolver;
    private final ObjectMapper objectMapper;

    public OrderService(OrdersRepo ordersRepo, HistoricalOrdersRepo historicalOrdersRepo, BuyOrderValidator buyOrderValidator, OrderPriceCalculator priceCalculator, AccountResolver accountResolver, InstrumentResolver instrumentResolver, ObjectMapper objectMapper) {
        this.ordersRepo = ordersRepo;
        this.historicalOrdersRepo = historicalOrdersRepo;
        this.buyOrderValidator = buyOrderValidator;
        this.priceCalculator = priceCalculator;
        this.accountResolver = accountResolver;
        this.instrumentResolver = instrumentResolver;
        this.objectMapper = objectMapper;
    }

    public OrderEntity createOrder(CreateOrderRequest request) {
        AccountsEntity account = accountResolver.resolve(request.accountId());
        InstrumentEntity instrument = instrumentResolver.resolve(request.instrumentId());

        String orderSide = request.side();
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        if ("BUY".equals(orderSide)) {
            buyOrderValidator.validate(request, account, instrument);
            totalPrice = priceCalculator.calculateOrderPrice(instrument, request.quantity());
        }
        else if ("SELL".equals(orderSide)) {
            // TODO: SellOrderValidator will be used here when available
        } else {
            // TODO: return an error response, invalid side
        }
        
        OrderEntity order = new OrderEntity(
            orderSide,
            account,
            instrument,
            request.quantity(),
            totalPrice
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
    public List<HistoricalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return historicalOrdersRepo.findByOrderId_OrderIdOrderByCreatedAtAsc(orderId);
    }

    /**
     * Capture an immutable snapshot of the order's current state in the historical_orders table.
     * 
     * The snapshot contains ONLY scalar values and plain IDs — no entity references.
     * This ensures that if the live order's status changes later, the historical record
     * still reflects what the status was at the time of this event.
     */
    private void captureOrderSnapshot(OrderEntity order) {
        // Build a snapshot containing only scalars and IDs, not entity objects
        OrderSnapshot snapshot = new OrderSnapshot(
            order.getStatus(),                              // status at this moment
            order.getSide(),
            order.getQuantity(),
            order.getTotalPrice(),
            order.getInstrumentId().getInstrumentId(),    // extract scalar ID from entity
            order.getAccountId().getAccountId(),          // extract scalar ID from entity
            order.getUpdatedAt()
        );
        
        String jsonSnapshot = serializeSnapshotToJson(snapshot);
        
        HistoricalOrdersEntity history = new HistoricalOrdersEntity();
        history.setOrderId(order);
        history.setAccount(order.getAccountId());
        history.setOrderInformationJson(jsonSnapshot);
        
        historicalOrdersRepo.save(history);
    }

    /**
     * Serialize an OrderSnapshot (not the full entity) to JSON string for storage.
     * Throws RuntimeException if serialization fails to trigger transaction rollback.
     */
    private String serializeSnapshotToJson(OrderSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order snapshot to JSON: {}", e.getMessage());
            throw new RuntimeException("Order snapshot serialization failed", e);
        }
    }
}
