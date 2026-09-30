package main.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import main.dto.request.CreateOrderRequest;
import main.dto.response.OrderSubmissionResponse;
import main.entities.OrderEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.dto.InstrumentWithPrice;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.InstrumentRepo;
import main.dto.response.OrderHistoryResponse;
import main.services.validation.BuyOrderValidator;
import main.services.validation.SellOrderValidator;
import main.services.calculation.OrderPriceCalculator;
import main.services.resolver.AccountResolver;
import main.services.resolver.InstrumentResolver;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;

import java.util.List;

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

        return new OrderSubmissionResponse(order.getOrderId(), order.getCreatedAt());
    }

    public List<OrderHistoryResponse> getOrderHistory(Integer accountId) {
        accountResolver.resolve(accountId);
        return ordersRepo.findOrdersByAccountId(accountId);
    }


    public List<main.entities.HistoricalOrdersEntity> getHistoricalOrders(Integer orderId) {
        return historicalOrdersService.getHistoricalOrders(orderId);
    }
}
