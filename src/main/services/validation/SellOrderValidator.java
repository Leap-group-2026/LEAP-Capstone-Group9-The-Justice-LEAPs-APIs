package services.validation;

import org.springframework.stereotype.Component;
import dto.request.CreateOrderRequest;
import entities.AccountsEntity;
import dto.InstrumentWithPrice;
import entities.PositionsEntity;
import exception.InvalidOrderException;
import repos.PositionsRepo;
import java.util.List;

@Component
public class SellOrderValidator {
    private final PositionsRepo positionsRepo;

    public SellOrderValidator(PositionsRepo positionsRepo) {
        this.positionsRepo = positionsRepo;
    }

    public void validate(CreateOrderRequest request, AccountsEntity account, InstrumentWithPrice instrument) {
        validateRequest(request);
        validateAccount(account);
        validateInstrument(instrument);
        validateQuantity(request.quantity());
        validateSufficientHoldings(account.getAccountId(), instrument.getInstrumentId(), request.quantity());
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new InvalidOrderException("request", "Order request cannot be null");
        }
    }

    private void validateAccount(AccountsEntity account) {
        if (account == null) {
            throw new InvalidOrderException("account", "Account not found");
        }
        
        if (account.getAccountId() == null || account.getAccountId() <= 0) {
            throw new InvalidOrderException("account", "Invalid account ID");
        }
    }

    private void validateInstrument(InstrumentWithPrice instrument) {
        if (instrument == null) {
            throw new InvalidOrderException("instrument", "Instrument not found");
        }
        
        if (instrument.getInstrumentId() == null || instrument.getInstrumentId() <= 0) {
            throw new InvalidOrderException("instrument", "Invalid instrument ID");
        }
        
        if (instrument.getPrice() == null) {
            throw new InvalidOrderException("instrument",
                "Instrument " + instrument.getTicker() + " has no current price");
        }

        if (instrument.getPrice().signum() <= 0) {
            throw new InvalidOrderException("instrument", "Instrument has invalid price");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new InvalidOrderException("quantity", "Quantity cannot be null");
        }
        
        if (quantity <= 0) {
            throw new InvalidOrderException("quantity", "Quantity must be greater than zero");
        }
    }

   private void validateSufficientHoldings(Integer accountId, Integer instrumentId, Integer quantityToSell) {
    List<PositionsEntity> positions = positionsRepo.findByAccount(accountId);
    
    PositionsEntity position = positions.stream()
        .filter(p -> p.getInstrumentId() != null && 
                    p.getInstrumentId().getInstrumentId().equals(instrumentId) &&
                    p.getClosedAt() == null)
        .findFirst()
        .orElse(null);
    
    if (position == null || position.getQuantity() == null) {
        throw new InvalidOrderException("holdings", 
            "Account does not have any open positions for this instrument");
    }
    
    if (position.getQuantity() < quantityToSell) {
        throw new InvalidOrderException("holdings", 
            String.format("Insufficient holdings. Current: %d, Attempting to sell: %d", 
                position.getQuantity(), quantityToSell));
        }
    }
}
