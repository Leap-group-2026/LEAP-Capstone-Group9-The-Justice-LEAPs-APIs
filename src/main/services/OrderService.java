package main.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import main.dto.request.CreateOrderRequest;
import main.entities.OrderEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.InstrumentRepo;
import main.services.validation.BuyOrderValidator;
import main.services.calculation.OrderPriceCalculator;
import main.services.resolver.AccountResolver;
import main.services.resolver.InstrumentResolver;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrdersRepo ordersRepo;
    private final HistoricalOrdersService historicalOrdersService;
    private final BuyOrderValidator buyOrderValidator;
    private final OrderPriceCalculator priceCalculator;
    private final AccountResolver accountResolver;
    private final InstrumentResolver instrumentResolver;

    public OrderService(OrdersRepo ordersRepo, HistoricalOrdersService historicalOrdersService, BuyOrderValidator buyOrderValidator, OrderPriceCalculator priceCalculator, AccountResolver accountResolver, InstrumentResolver instrumentResolver) {
        this.ordersRepo = ordersRepo;
        this.historicalOrdersService = historicalOrdersService;
        this.buyOrderValidator = buyOrderValidator;
        this.priceCalculator = priceCalculator;
        this.accountResolver = accountResolver;
        this.instrumentResolver = instrumentResolver;
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
     * Create a new order and capture an initial snapshot using MyBatis.
     * Both the order insert and the snapshot insert happen atomically.
     */
    @Transactional
    public OrderEntity createOrderWithSnapshot(OrderEntity newOrder) {
        // Set scalar ID values from entity references for MyBatis insert
        if (newOrder.getAccountId() != null) {
            newOrder.setAccountIdValue(newOrder.getAccountId().getAccountId());
        }
        if (newOrder.getInstrumentId() != null) {
            newOrder.setInstrumentIdValue(newOrder.getInstrumentId().getInstrumentId());
        }
        
        // Save the new order using MyBatis insert (will auto-generate orderId)
        ordersRepo.insert(newOrder);
        
        // Capture the initial state as a snapshot
        historicalOrdersService.captureOrderSnapshot(newOrder);
        
        log.info("Order {} created with initial snapshot", newOrder.getOrderId());
        return newOrder;
    }

    /**
     * Update an order's status and record immutable snapshots (before and after).
     * Both updates happen atomically: if one fails, both roll back.
     */
    @Transactional
    public OrderEntity updateOrderStatus(Integer orderId, String newStatus) {
        OrderEntity order = ordersRepo.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        log.info("=== updateOrderStatus START === orderId={} currentStatus={}", orderId, order.getStatus());
        
        // Capture the OLD state (before change) — BEFORE mutating
        log.info("Capturing BEFORE snapshot: status={}", order.getStatus());
        historicalOrdersService.captureOrderSnapshot(order);
        
        // Mutate the order in memory
        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());  // IMPORTANT: Update the timestamp
        
        // Update using MyBatis (set scalar IDs for update)
        if (order.getAccountId() != null) {
            order.setAccountIdValue(order.getAccountId().getAccountId());
        }
        if (order.getInstrumentId() != null) {
            order.setInstrumentIdValue(order.getInstrumentId().getInstrumentId());
        }
        
        log.info("Calling ordersRepo.update() with status={} updatedAt={}", order.getStatus(), order.getUpdatedAt());
        ordersRepo.update(order);
        log.info("ordersRepo.update() completed");
        
        // Now capture the NEW state — use the mutated in-memory object with the new status
        // This is the fresh state we just persisted
        log.info("Capturing AFTER snapshot: status={} (from just-persisted state)", order.getStatus());
        historicalOrdersService.captureOrderSnapshot(order);
        
        log.info("=== updateOrderStatus END === Order {} status changed from {} to {}", orderId, oldStatus, newStatus);
        return order;
    }

    /**
     * Retrieve all historical snapshots for an order in chronological order.
     */
    public List<main.entities.HistoricalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return historicalOrdersService.getHistoricalOrders(orderId);
    }
}
