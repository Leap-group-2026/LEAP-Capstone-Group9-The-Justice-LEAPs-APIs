package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import main.services.validation.BuyOrderValidator;
import main.services.calculation.OrderPriceCalculator;
import main.dto.request.CreateOrderRequest;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.exception.InvalidOrderException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BuyOrderValidatorTest {
    
    private BuyOrderValidator validator;
    private OrderPriceCalculator priceCalculator;
    
    @BeforeEach
    void setUp() {
        priceCalculator = new OrderPriceCalculator();
        validator = new BuyOrderValidator(priceCalculator);
    }
    
    private AccountsEntity createAccount(BigDecimal balance) {
        AccountsEntity account = new AccountsEntity();
        account.setBalance(balance);
        return account;
    }
    
    private InstrumentEntity createInstrument(BigDecimal price) {
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setPrice(price);
        return instrument;
    }
    
    @Test
    void testValidate_BothValidationsPass() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(2000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        // Create a ZonedDateTime during market hours (12:00 PM ET)
        ZonedDateTime marketHoursTime = ZonedDateTime.of(2024, 9, 24, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        
        assertDoesNotThrow(() -> {
            validator.validate(request, account, instrument, marketHoursTime);
        });
    }

    @Test
    void testValidateBalance_InsufficientBalance() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(500.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        ZonedDateTime marketHoursTime = ZonedDateTime.of(2024, 9, 24, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        
        InvalidOrderException exception = assertThrows(InvalidOrderException.class, () -> {
            validator.validate(request, account, instrument, marketHoursTime);
        });
        
        assertEquals("balance", exception.getField());
        assertTrue(exception.getReason().contains("Insufficient balance"));
        assertTrue(exception.getReason().contains("500"));
        assertTrue(exception.getReason().contains("1000"));
    }
    
    @Test
    void testValidateBalance_BalanceEqualsOrderPrice() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(1000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        ZonedDateTime marketHoursTime = ZonedDateTime.of(2024, 9, 24, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        
        assertDoesNotThrow(() -> {
            validator.validate(request, account, instrument, marketHoursTime);
        });
    }
    
    @Test
    void testValidateBalance_BalanceExceedsOrderPrice() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(2000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        ZonedDateTime marketHoursTime = ZonedDateTime.of(2024, 9, 24, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        
        assertDoesNotThrow(() -> {
            validator.validate(request, account, instrument, marketHoursTime);
        });
    }

    @Test
    void testValidateMarketHours_BeforeMarketOpen() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(2000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        // 9:29:59 AM ET (before market open)
        ZonedDateTime beforeMarketOpen = ZonedDateTime.of(2024, 9, 24, 9, 29, 59, 0, ZoneId.of("America/New_York"));
        
        InvalidOrderException exception = assertThrows(InvalidOrderException.class, () -> {
            validator.validate(request, account, instrument, beforeMarketOpen);
        });
        
        assertEquals("market_hours", exception.getField());
        assertTrue(exception.getReason().contains("Market is not open"));
    }
    
    @Test
    void testValidateMarketHours_ExactlyAtMarketOpen() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(2000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        // Exactly 9:30:00 AM ET (market opens)
        ZonedDateTime exactlyAtOpen = ZonedDateTime.of(2024, 9, 24, 9, 30, 0, 0, ZoneId.of("America/New_York"));
        
        assertDoesNotThrow(() -> {
            validator.validate(request, account, instrument, exactlyAtOpen);
        });
    }
    
    @Test
    void testValidateMarketHours_DuringMarketHours() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(2000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        // 12:00:00 PM ET (mid-day during market hours)
        ZonedDateTime midDay = ZonedDateTime.of(2024, 9, 24, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        
        assertDoesNotThrow(() -> {
            validator.validate(request, account, instrument, midDay);
        });
    }
    
    @Test
    void testValidateMarketHours_ExactlyAtMarketClose() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(2000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        // Exactly 4:00:00 PM ET (market close, inclusive boundary)
        ZonedDateTime exactlyAtClose = ZonedDateTime.of(2024, 9, 24, 16, 0, 0, 0, ZoneId.of("America/New_York"));
        
        assertDoesNotThrow(() -> {
            validator.validate(request, account, instrument, exactlyAtClose);
        });
    }
    
    @Test
    void testValidateMarketHours_AfterMarketClose() {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(2000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        // 4:00:01 PM ET (after market close)
        ZonedDateTime afterClose = ZonedDateTime.of(2024, 9, 24, 16, 0, 1, 0, ZoneId.of("America/New_York"));
        
        InvalidOrderException exception = assertThrows(InvalidOrderException.class, () -> {
            validator.validate(request, account, instrument, afterClose);
        });
        
        assertEquals("market_hours", exception.getField());
        assertTrue(exception.getReason().contains("Market is not open"));
    }

    @ParameterizedTest
    @MethodSource("provideTimezoneTestCases")
    void testIsMarketOpen_TimezoneConversions(String testName, ZonedDateTime zonedTime, boolean expectedResult) {
        CreateOrderRequest request = new CreateOrderRequest("BUY", 1, 1, 100);
        AccountsEntity account = createAccount(BigDecimal.valueOf(2000.00));
        InstrumentEntity instrument = createInstrument(BigDecimal.valueOf(10.00));
        
        if (expectedResult) {
            assertDoesNotThrow(() -> {
                validator.validate(request, account, instrument, zonedTime);
            }, "Test: " + testName);
        } else {
            InvalidOrderException exception = assertThrows(InvalidOrderException.class, () -> {
                validator.validate(request, account, instrument, zonedTime);
            }, "Test: " + testName);
            
            assertEquals("market_hours", exception.getField());
        }
    }
    
    static Stream<org.junit.jupiter.params.provider.Arguments> provideTimezoneTestCases() {
        return Stream.of(
            // UTC to ET conversions
            // 1:30 PM UTC = 9:30 AM ET (market open)
            org.junit.jupiter.params.provider.Arguments.of(
                "UTC 1:30 PM (9:30 AM ET) - Market Open",
                ZonedDateTime.of(2024, 9, 24, 13, 30, 0, 0, ZoneId.of("UTC")),
                true
            ),
            
            // 8:00 PM UTC = 4:00 PM ET (market close)
            org.junit.jupiter.params.provider.Arguments.of(
                "UTC 8:00 PM (4:00 PM ET) - Market Close",
                ZonedDateTime.of(2024, 9, 24, 20, 0, 0, 0, ZoneId.of("UTC")),
                true
            ),
            
            // PST to ET conversions
            // 6:30 AM PST = 9:30 AM ET (market open) - using PDT for daylight saving
            org.junit.jupiter.params.provider.Arguments.of(
                "PST 6:30 AM (9:30 AM ET) - Market Open",
                ZonedDateTime.of(2024, 9, 24, 6, 30, 0, 0, ZoneId.of("America/Los_Angeles")),
                true
            ),
            
            // 1:00 PM PST = 4:00 PM ET (market close)
            org.junit.jupiter.params.provider.Arguments.of(
                "PST 1:00 PM (4:00 PM ET) - Market Close",
                ZonedDateTime.of(2024, 9, 24, 13, 0, 0, 0, ZoneId.of("America/Los_Angeles")),
                true
            ),
            
            // CST to ET conversions
            // 11:00 AM CST = 12:00 PM ET (mid-day)
            org.junit.jupiter.params.provider.Arguments.of(
                "CST 11:00 AM (12:00 PM ET) - Mid-day",
                ZonedDateTime.of(2024, 9, 24, 11, 0, 0, 0, ZoneId.of("America/Chicago")),
                true
            ),
            
            // EST to ET conversions (already in ET)
            // 10:00 AM EST = 10:00 AM ET
            org.junit.jupiter.params.provider.Arguments.of(
                "EST 10:00 AM (10:00 AM ET) - Already ET",
                ZonedDateTime.of(2024, 9, 24, 10, 0, 0, 0, ZoneId.of("America/New_York")),
                true
            ),
            
            // JST to ET conversions (same day conversion)
            // 12:30 AM JST (next day) = 11:30 AM previous day ET
            org.junit.jupiter.params.provider.Arguments.of(
                "JST 12:30 AM (11:30 AM previous day ET) - Cross-day",
                ZonedDateTime.of(2024, 9, 25, 0, 30, 0, 0, ZoneId.of("Asia/Tokyo")),
                true
            )
        );
    }
}
