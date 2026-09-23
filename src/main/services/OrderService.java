package main.services;

import org.springframework.stereotype.Service;

import main.dto.request.CreateOrderRequest;
import main.entities.OrderEntity;
import main.entities.accountsEntity;
import main.entities.instrumentEntity;
import main.repos.OrdersRepo;
import main.services.validation.BuyOrderValidator;
import main.services.calculation.OrderPriceCalculator;
import main.services.resolver.AccountResolver;
import main.services.resolver.InstrumentResolver;
import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrdersRepo ordersRepo;
    private final BuyOrderValidator buyOrderValidator;
    private final OrderPriceCalculator priceCalculator;
    private final AccountResolver accountResolver;
    private final InstrumentResolver instrumentResolver;

    public OrderService(OrdersRepo ordersRepo, BuyOrderValidator buyOrderValidator, 
                       OrderPriceCalculator priceCalculator, AccountResolver accountResolver,
                       InstrumentResolver instrumentResolver) {
        this.ordersRepo = ordersRepo;
        this.buyOrderValidator = buyOrderValidator;
        this.priceCalculator = priceCalculator;
        this.accountResolver = accountResolver;
        this.instrumentResolver = instrumentResolver;
    }

    public OrderEntity createOrder(CreateOrderRequest request) {
        accountsEntity account = accountResolver.resolve(request.accountId());
        instrumentEntity instrument = instrumentResolver.resolve(request.instrumentId());

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

        return ordersRepo.save(order);
    }
}
