package main.services.validation;

import org.springframework.stereotype.Component;
import main.dto.request.CreateOrderRequest;
import main.entities.accountsEntity;
import main.entities.instrumentEntity;
import main.exception.InvalidOrderException;

@Component
public class BuyOrderValidator {
    public void validate(CreateOrderRequest request, accountsEntity account, instrumentEntity instrument) {
        // TODO: implement validation logic for buy orders
    }
}
