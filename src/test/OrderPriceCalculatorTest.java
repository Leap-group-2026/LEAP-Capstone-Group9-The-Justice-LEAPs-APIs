package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import main.services.calculation.OrderPriceCalculator;
import main.entities.InstrumentEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderPriceCalculatorTest {
    
    private OrderPriceCalculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new OrderPriceCalculator();
    }

    @Test
    void testCalculateTotalPrice() {
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setPrice(BigDecimal.valueOf(0.50));
        
        BigDecimal result = calculator.calculateOrderPrice(instrument, 100);
        
        assertEquals(BigDecimal.valueOf(50.00), result);
    }
}
