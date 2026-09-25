package main.services;

import org.springframework.stereotype.Service;

import main.dto.request.CreateOrderRequest;
import main.dto.response.OrderSubmissionResponse;
import main.entities.OrderEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.repos.OrdersRepo;
import main.services.validation.BuyOrderValidator;
import main.services.validation.SellOrderValidator;
import main.services.calculation.OrderPriceCalculator;
import main.services.resolver.AccountResolver;
import main.services.resolver.InstrumentResolver;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service
public class OrderService {
    private final OrdersRepo ordersRepo;
    private final BuyOrderValidator buyOrderValidator;
    private final SellOrderValidator sellOrderValidator;
    private final AccountResolver accountResolver;
    private final InstrumentResolver instrumentResolver;

    public OrderService(OrdersRepo ordersRepo, BuyOrderValidator buyOrderValidator, SellOrderValidator sellOrderValidator,
                       AccountResolver accountResolver, InstrumentResolver instrumentResolver) {
        this.ordersRepo = ordersRepo;
        this.buyOrderValidator = buyOrderValidator;
        this.sellOrderValidator = sellOrderValidator;
        this.accountResolver = accountResolver;
        this.instrumentResolver = instrumentResolver;
    }

    public OrderSubmissionResponse createOrder(CreateOrderRequest request) {
        AccountsEntity account = accountResolver.resolve(request.accountId());
        InstrumentEntity instrument = instrumentResolver.resolve(request.instrumentId());

        String orderSide = request.side();
        
        if ("BUY".equals(orderSide)) {
            ZonedDateTime submittedAt = ZonedDateTime.now();
            buyOrderValidator.validate(request, account, instrument, submittedAt);
        }
        else if ("SELL".equals(orderSide)) {
            sellOrderValidator.validate(request, account, instrument);
        } else {
            // TODO: return an error response, invalid side
            throw new IllegalArgumentException("Invalid order: " + orderSide);
        }

        BigDecimal totalPrice = OrderPriceCalculator.calculateOrderPrice(instrument, request.quantity());

        OrderEntity order = new OrderEntity();
        order.setSide(orderSide);
        order.setAccountId(account);
        order.setInstrumentId(instrument);
        order.setStatus("PENDING");
        order.setQuantity(request.quantity());
        order.setTotalPrice(totalPrice);

        ordersRepo.insert(order);

        return new OrderSubmissionResponse(order.getOrderId(), order.getCreatedAt());
    }
}
