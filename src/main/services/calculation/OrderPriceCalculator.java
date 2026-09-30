package main.services.calculation;

import org.springframework.stereotype.Service;
import main.dto.InstrumentWithPrice;
import main.exception.InvalidOrderException;
import java.math.BigDecimal;

@Service
public class OrderPriceCalculator {

    public static BigDecimal calculateOrderPrice(InstrumentWithPrice instrument, Integer quantity) {
        if (instrument.getPrice() == null) {
            throw new InvalidOrderException(
                "price",
                "Cannot calculate order price: instrument " + instrument.getTicker() + " has no current price"
            );
        }
        return instrument.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public static BigDecimal calculateExecutionTotal(BigDecimal currentPrice, Integer quantity) {
        return currentPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    public BigDecimal calculate(InstrumentWithPrice instrument, Integer quantity) {
        return calculateOrderPrice(instrument, quantity);
    }
}
