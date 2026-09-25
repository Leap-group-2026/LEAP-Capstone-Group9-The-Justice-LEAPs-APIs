package main.services.calculation;

import main.entities.InstrumentEntity;
import java.math.BigDecimal;

public class OrderPriceCalculator {

    public static BigDecimal calculateOrderPrice(InstrumentEntity instrument, Integer quantity) {
        BigDecimal instrumentPrice = instrument.getPrice();

        return instrumentPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
