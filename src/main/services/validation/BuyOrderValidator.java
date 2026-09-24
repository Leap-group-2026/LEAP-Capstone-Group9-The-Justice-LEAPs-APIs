package main.services.validation;

import org.springframework.stereotype.Component;
import main.dto.request.CreateOrderRequest;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.exception.InvalidOrderException;

@Component
public class BuyOrderValidator {
    public void validate(CreateOrderRequest request, AccountsEntity account, InstrumentEntity instrument) {
        // TODO: implement validation logic for buy orders
    }
}
