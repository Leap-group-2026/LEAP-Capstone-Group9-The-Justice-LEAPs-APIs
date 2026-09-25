package main.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import main.dto.request.CreateOrderRequest;
import main.dto.response.OrderSubmissionResponse;
import main.entities.OrderEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.InstrumentRepo;
import main.services.validation.BuyOrderValidator;
import main.services.validation.SellOrderValidator;
import main.services.calculation.OrderPriceCalculator;
import main.services.resolver.AccountResolver;
import main.services.resolver.InstrumentResolver;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import java.time.ZonedDateTime;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrdersRepo ordersRepo;
    private final HistoricalOrdersService historicalOrdersService;
    private final BuyOrderValidator buyOrderValidator;
    private final SellOrderValidator sellOrderValidator;
    private final OrderPriceCalculator priceCalculator;
    private final AccountResolver accountResolver;
    private final InstrumentResolver instrumentResolver;
    private final Clock clock;

    public OrderService(OrdersRepo ordersRepo, HistoricalOrdersService historicalOrdersService, BuyOrderValidator buyOrderValidator, SellOrderValidator sellOrderValidator,
                       OrderPriceCalculator priceCalculator, AccountResolver accountResolver, InstrumentResolver instrumentResolver, Clock clock) {
        this.ordersRepo = ordersRepo;
        this.historicalOrdersService = historicalOrdersService;
        this.buyOrderValidator = buyOrderValidator;
        this.sellOrderValidator = sellOrderValidator;
        this.priceCalculator = priceCalculator;
        this.accountResolver = accountResolver;
        this.instrumentResolver = instrumentResolver;
        this.clock = clock;
    }

    public OrderSubmissionResponse createOrder(CreateOrderRequest request) {
        AccountsEntity account = accountResolver.resolve(request.accountId());
        InstrumentEntity instrument = instrumentResolver.resolve(request.instrumentId());

        String orderSide = request.side();
        
        if ("BUY".equals(orderSide)) {
            ZonedDateTime submittedAt = ZonedDateTime.now(clock);
            buyOrderValidator.validate(request, account, instrument, submittedAt);
        }
        else if ("SELL".equals(orderSide)) {
            sellOrderValidator.validate(request, account, instrument);
        } else {
            // TODO: return an error response, invalid side
            throw new IllegalArgumentException("Invalid order: " + orderSide);
        }

        BigDecimal totalPrice = priceCalculator.calculateOrderPrice(instrument, request.quantity());

        OrderEntity order = new OrderEntity();
        order.setSide(orderSide);
        order.setAccountId(account);
        order.setInstrumentId(instrument);
        order.setStatus("PENDING");
        order.setQuantity(request.quantity());
        order.setTotalPrice(totalPrice);

        ordersRepo.insert(order);
        
        // Extract scalar IDs from entities for snapshot capture
        order.setAccountIdValue(account.getAccountId());
        order.setInstrumentIdValue(instrument.getInstrumentId());
        
        // Capture initial snapshot of order for history tracking
        historicalOrdersService.captureOrderSnapshot(order);

        return new OrderSubmissionResponse(order.getOrderId(), order.getCreatedAt());
    }

    /**
     * Retrieve all historical snapshots for an order in chronological order.
     */
    public List<main.entities.HistoricalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return historicalOrdersService.getHistoricalOrders(orderId);
    }
}
