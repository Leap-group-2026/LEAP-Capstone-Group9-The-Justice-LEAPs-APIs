package main.services.calculation;

import org.springframework.stereotype.Component;
import main.entities.InstrumentEntity;
import java.math.BigDecimal;

@Component
public class OrderPriceCalculator {
    
    public BigDecimal calculateOrderPrice(InstrumentEntity instrument, Integer quantity) {
        BigDecimal instrumentPrice = instrument.getPrice();

        return instrumentPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
