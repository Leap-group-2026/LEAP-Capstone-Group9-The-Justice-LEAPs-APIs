package main.services.calculation;

import org.springframework.stereotype.Service;
import main.entities.InstrumentEntity;
import java.math.BigDecimal;

@Service
public class OrderPriceCalculator {

    public static BigDecimal calculateOrderPrice(InstrumentEntity instrument, Integer quantity) {
        BigDecimal instrumentPrice = instrument.getPrice();

        return instrumentPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
  
    public BigDecimal calculate(InstrumentEntity instrument, Integer quantity) {
        return calculateOrderPrice(instrument, quantity);
    }
}
