package main.services;

import org.springframework.stereotype.Service;

import main.dto.request.CreateOrderRequest;
import main.entities.OrderEntity;
import main.entities.accountsEntity;
import main.entities.instrumentEntity;
import main.exception.ResourceNotFoundException;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.instrumentRepo;

// TODO: delete import after implementing order total calculation
import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrdersRepo ordersRepo;
    private final AccountsRepo accountsRepo;
    private final instrumentRepo instrumentRepo;

    public OrderService(OrdersRepo ordersRepo, AccountsRepo accountsRepo, instrumentRepo instrumentRepo) {
        this.ordersRepo = ordersRepo;
        this.accountsRepo = accountsRepo;
        this.instrumentRepo = instrumentRepo;
    }

    public OrderEntity createOrder(CreateOrderRequest request) {
        // TODO: separate this out to different methods and create custom exceptions
        accountsEntity account = accountsRepo.findById(request.accountId())
            .orElseThrow(() -> new ResourceNotFoundException("Account", request.accountId().toString()));

        instrumentEntity instrument = instrumentRepo.findById(request.instrumentId())
            .orElseThrow(() -> new ResourceNotFoundException("Instrument", request.instrumentId().toString()));

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
