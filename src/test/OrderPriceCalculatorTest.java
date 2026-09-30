package test;

import org.junit.jupiter.api.Test;

import main.services.calculation.OrderPriceCalculator;
import main.dto.InstrumentWithPrice;
import main.exception.InvalidOrderException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class OrderPriceCalculatorTest {

    @Test
    void testCalculateTotalPrice() {
        InstrumentWithPrice instrument = new InstrumentWithPrice();
        instrument.setInstrumentId(1);
        instrument.setTicker("AAPL");
        instrument.setAssetType("STOCK");
        instrument.setAssetName("Apple Inc.");
        instrument.setCurrency("USD");
        instrument.setPrice(BigDecimal.valueOf(0.50));
        instrument.setQuoteTime(OffsetDateTime.now(ZoneOffset.UTC));
        
        BigDecimal result = OrderPriceCalculator.calculateOrderPrice(instrument, 100);
        
        assertEquals(BigDecimal.valueOf(50.00), result);
    }

    @Test
    void testCalculateTotalPrice_NoCurrentPrice() {
        InstrumentWithPrice instrument = new InstrumentWithPrice();
        instrument.setInstrumentId(1);
        instrument.setTicker("AAPL");

        InvalidOrderException exception = assertThrows(InvalidOrderException.class,
            () -> OrderPriceCalculator.calculateOrderPrice(instrument, 100));

        assertTrue(exception.getReason().contains("no current price"));
    }
}
