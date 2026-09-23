package main.services;

import org.springframework.stereotype.Service;

import main.dto.request.CreateOrderRequest;
import main.entities.OrderEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.InstrumentRepo;

// TODO: delete import after implementing order total calculation
import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrdersRepo ordersRepo;
    private final AccountsRepo accountsRepo;
    private final InstrumentRepo instrumentRepo;

    public OrderService(OrdersRepo ordersRepo, AccountsRepo accountsRepo, InstrumentRepo instrumentRepo) {
        this.ordersRepo = ordersRepo;
        this.accountsRepo = accountsRepo;
        this.instrumentRepo = instrumentRepo;
    }

    public OrderEntity createOrder(CreateOrderRequest request) {
        // TODO: separate this out to different methods and create custom exceptions
        AccountsEntity account = accountsRepo.findById(request.accountId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        InstrumentEntity instrument = instrumentRepo.findById(request.instrumentId())
            .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));

        // TODO: add validation later on before creating entities
        OrderEntity order = new OrderEntity(
            request.side(),
            account,
            instrument,
            request.quantity(),
            BigDecimal.valueOf(420.69)
        );

        return ordersRepo.save(order);
    }
}
