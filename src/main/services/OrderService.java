package main.services;

import org.springframework.stereotype.Service;

import main.dto.request.CreateOrderRequest;
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

@Service
public class OrderService {
    private final OrdersRepo ordersRepo;
    private final BuyOrderValidator buyOrderValidator;
    private final SellOrderValidator sellOrderValidator;
    private final OrderPriceCalculator priceCalculator;
    private final AccountResolver accountResolver;
    private final InstrumentResolver instrumentResolver;

    public OrderService(OrdersRepo ordersRepo, BuyOrderValidator buyOrderValidator, SellOrderValidator sellOrderValidator,
                       OrderPriceCalculator priceCalculator, AccountResolver accountResolver,
                       InstrumentResolver instrumentResolver) {
        this.ordersRepo = ordersRepo;
        this.buyOrderValidator = buyOrderValidator;
        this.sellOrderValidator = sellOrderValidator;
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
            sellOrderValidator.validate(request, account, instrument);
            totalPrice = priceCalculator.calculateOrderPrice(instrument, request.quantity());
        } else {
            // TODO: return an error response, invalid side
            throw new IllegalArgumentException("Invalid order: " + orderSide);
        }
        
        OrderEntity order = new OrderEntity(
            orderSide,
            account,
            instrument,
            request.quantity(),
            totalPrice
        );

        ordersRepo.insert(orderSide, account.getAccountId(), instrument.getInstrumentId(), 
                         "PENDING", request.quantity(), totalPrice, order.getCreatedAt(), order.getUpdatedAt());
        return order;
    }
}
