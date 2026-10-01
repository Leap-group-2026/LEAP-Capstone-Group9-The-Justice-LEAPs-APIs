package services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dto.request.CreateOrderRequest;
import dto.response.OrderSubmissionResponse;
import entities.OrderEntity;
import entities.AccountsEntity;
import entities.InstrumentEntity;
import entities.HistoricalOrdersEntity;
import dto.InstrumentWithPrice;
import repos.OrdersRepo;
import repos.AccountsRepo;
import repos.InstrumentRepo;
import dto.response.OrderHistoryResponse;
import events.OrderSubmittedEvent;
import services.validation.BuyOrderValidator;
import services.validation.SellOrderValidator;
import services.calculation.OrderPriceCalculator;
import services.resolver.AccountResolver;
import services.resolver.InstrumentResolver;
import exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;

import java.util.List;
import java.util.UUID;

import java.time.ZonedDateTime;

@Service
public class OrderService {
    private final OrdersRepo ordersRepo;
    private final HistoricalOrdersService historicalOrdersService;
    private final BuyOrderValidator buyOrderValidator;
    private final SellOrderValidator sellOrderValidator;
    private final OrderPriceCalculator priceCalculator;
    private final AccountResolver accountResolver;
    private final InstrumentResolver instrumentResolver;
    private final InstrumentRepo instrumentRepo;
    private final Clock clock;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderService(OrdersRepo ordersRepo, HistoricalOrdersService historicalOrdersService, BuyOrderValidator buyOrderValidator, SellOrderValidator sellOrderValidator,
                       OrderPriceCalculator priceCalculator, AccountResolver accountResolver, InstrumentResolver instrumentResolver, InstrumentRepo instrumentRepo, Clock clock,
                       ApplicationEventPublisher applicationEventPublisher) {
        this.ordersRepo = ordersRepo;
        this.historicalOrdersService = historicalOrdersService;
        this.buyOrderValidator = buyOrderValidator;
        this.sellOrderValidator = sellOrderValidator;
        this.priceCalculator = priceCalculator;
        this.accountResolver = accountResolver;
        this.instrumentResolver = instrumentResolver;
        this.instrumentRepo = instrumentRepo;
        this.clock = clock;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    // The order and its historical_orders snapshot commit together or not at all: the snapshot is a
    // regulatory audit record, so an order must never exist without one
    @Transactional
    public OrderSubmissionResponse createOrder(CreateOrderRequest request) {
        AccountsEntity account = accountResolver.resolve(request.accountId());
        InstrumentWithPrice instrument = instrumentResolver.resolve(request.instrumentId());

        String orderSide = request.side();
        
        if ("BUY".equals(orderSide)) {
            ZonedDateTime submittedAt = ZonedDateTime.now(clock);
            buyOrderValidator.validate(request, account, instrument, submittedAt);
        }
        else if ("SELL".equals(orderSide)) {
            sellOrderValidator.validate(request, account, instrument);
        } else {

            throw new IllegalArgumentException("Invalid order: " + orderSide);
        }

        BigDecimal totalPrice = priceCalculator.calculateOrderPrice(instrument, request.quantity());

        OrderEntity order = new OrderEntity();
        order.setSide(orderSide);
        order.setAccountId(account);
        InstrumentEntity instrumentRef = new InstrumentEntity();
        instrumentRef.setInstrumentId(instrument.getInstrumentId());
        order.setInstrumentId(instrumentRef);
        order.setStatus("PENDING");
        order.setQuantity(request.quantity());
        order.setTotalPrice(totalPrice);

        ordersRepo.insert(order);

        historicalOrdersService.captureOrderSnapshot(order);

        applicationEventPublisher.publishEvent(new OrderSubmittedEvent(
            UUID.randomUUID(),
            order.getOrderId(),
            account.getAccountId(),
            instrument.getInstrumentId(),
            order.getSide(),
            order.getQuantity(),
            order.getTotalPrice(),
            order.getCreatedAt(),
            1));

        return new OrderSubmissionResponse(order.getOrderId(), order.getCreatedAt());
    }

    public List<OrderHistoryResponse> getOrderHistory(Integer accountId) {
        accountResolver.resolve(accountId);
        return ordersRepo.findOrdersByAccountId(accountId);
    }


    public List<HistoricalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return historicalOrdersService.getHistoricalOrders(orderId);
    }

    public OrderHistoryResponse getByOrderId(Integer orderId)
    {
        OrderEntity order = ordersRepo.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        InstrumentEntity instrument = instrumentRepo.findEntityById(order.getInstrumentId().getInstrumentId());
        OrderHistoryResponse response = new OrderHistoryResponse();
        response.setOrderId(order.getOrderId());
        response.setTicker(instrument.getTicker());
        response.setSide(order.getSide());
        response.setQuantity(order.getQuantity());
        response.setPricePerUnit(order.getTotalPrice().divide(BigDecimal.valueOf(order.getQuantity())));
        response.setStatus(order.getStatus());
        response.setTotalPrice(order.getTotalPrice());
        if ("FILLED".equals(order.getStatus())) {
            response.setExecutedAt(order.getUpdatedAt());
        }
        return response;
    }
}
