package main.services.validation;

import org.springframework.stereotype.Component;
import main.dto.request.CreateOrderRequest;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.entities.PositionsEntity;
import main.exception.InvalidOrderException;
import main.repos.PositionsRepo;
import java.util.List;

@Component
public class SellOrderValidator {
    private final PositionsRepo positionsRepo;

    public SellOrderValidator(PositionsRepo positionsRepo) {
        this.positionsRepo = positionsRepo;
    }

    public void validate(CreateOrderRequest request, AccountsEntity account, InstrumentEntity instrument) {
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

    private void validateInstrument(InstrumentEntity instrument) {
        if (instrument == null) {
            throw new InvalidOrderException("instrument", "Instrument not found");
        }
        
        if (instrument.getInstrumentId() == null || instrument.getInstrumentId() <= 0) {
            throw new InvalidOrderException("instrument", "Invalid instrument ID");
        }
        
        if (instrument.getPrice() == null || instrument.getPrice().signum() <= 0) {
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
        
        // Find the position for this specific instrument
        PositionsEntity position = positions.stream()
            .filter(p -> p.getInstrumentId() != null && 
                        p.getInstrumentId().getInstrumentId() != null &&
                        p.getInstrumentId().getInstrumentId().equals(instrumentId) &&
                        p.getClosedAt() == null) // Only consider open positions
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
