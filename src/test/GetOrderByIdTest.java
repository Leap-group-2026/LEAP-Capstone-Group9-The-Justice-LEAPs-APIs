package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import main.Application;
import test.config.TestClockConfig;

/**
 * GET /orders/{orderId}, verified end to end.
 * 
 * Tests the OrderHistoryResponse DTO for single order retrieval with resolved instrument ticker
 * and optional executedAt timestamp when order is FILLED.
 * Fixtures are inserted with raw SQL through JdbcTemplate, never through the code under test.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("GET /orders/{orderId} Tests")
public class GetOrderByIdTest {

    private static final int USER_ID = 900601;
    private static final int ACCOUNT_ID = 900601;
    private static final int INSTRUMENT_ID = 900601;
    private static final int FILLED_ORDER_ID = 900701;
    private static final int PENDING_ORDER_ID = 900702;
    private static final int CANCELED_ORDER_ID = 900703;
    private static final int NONEXISTENT_ORDER_ID = 999999;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Create test user
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Order Test User', 'order.test@example.com', DATE '1990-01-01', '1 Test St', 'SSN_HASH', 'PASS_HASH')",
            USER_ID);

        // Create test account
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, '10000.0000', 'BALANCED', 'Passive', now(), true)",
            ACCOUNT_ID, USER_ID);

        // Create test instrument
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'GOOG', 'STOCK', 'Alphabet Inc.', 'USD')",
            INSTRUMENT_ID);

        // Insert current price for the instrument
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) " +
            "VALUES (?, '150.0000', now(), now())",
            INSTRUMENT_ID);

        // Create FILLED order with execution time
        insertOrder(FILLED_ORDER_ID, ACCOUNT_ID, INSTRUMENT_ID, "BUY", "FILLED", 
                   10, "1500.0000", "2026-01-01 10:00:00", "2026-01-01 10:05:00");
        
        // Create PENDING order with no execution time
        insertOrder(PENDING_ORDER_ID, ACCOUNT_ID, INSTRUMENT_ID, "SELL", "PENDING", 
                   5, "750.0000", "2026-01-02 11:00:00", "2026-01-02 11:00:00");
        
        // Create CANCELED order with no execution time
        insertOrder(CANCELED_ORDER_ID, ACCOUNT_ID, INSTRUMENT_ID, "BUY", "CANCELED", 
                   3, "450.0000", "2026-01-03 09:00:00", "2026-01-03 09:30:00");
    }

    private void insertOrder(int orderId, int accountId, int instrumentId, String side, String status,
                             int quantity, String totalPrice, String createdAt, String updatedAt) {
        jdbcTemplate.update(
            "INSERT INTO orders (order_id, side, account_id, instrument_id, status, quantity, total_price, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS NUMERIC(18,4)), CAST(? AS TIMESTAMP), CAST(? AS TIMESTAMP))",
            orderId, side, accountId, instrumentId, status, quantity, totalPrice, createdAt, updatedAt);
    }

    @Test
    @DisplayName("Should return filled order with all fields including executedAt")
    void returnFilledOrderWithExecutedAt() throws Exception {
        mockMvc.perform(get("/orders/" + FILLED_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(FILLED_ORDER_ID))
            .andExpect(jsonPath("$.ticker").value("GOOG"))
            .andExpect(jsonPath("$.side").value("BUY"))
            .andExpect(jsonPath("$.quantity").value(10))
            .andExpect(jsonPath("$.pricePerUnit").value(150.0))
            .andExpect(jsonPath("$.totalPrice").value(1500.0))
            .andExpect(jsonPath("$.status").value("FILLED"))
            .andExpect(jsonPath("$.executedAt").value("2026-01-01T10:05:00"));
    }

    @Test
    @DisplayName("Should return pending order without executedAt")
    void returnPendingOrderWithoutExecutedAt() throws Exception {
        mockMvc.perform(get("/orders/" + PENDING_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(PENDING_ORDER_ID))
            .andExpect(jsonPath("$.ticker").value("GOOG"))
            .andExpect(jsonPath("$.side").value("SELL"))
            .andExpect(jsonPath("$.quantity").value(5))
            .andExpect(jsonPath("$.pricePerUnit").value(150.0))
            .andExpect(jsonPath("$.totalPrice").value(750.0))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.executedAt").value(nullValue()));
    }

    @Test
    @DisplayName("Should return canceled order without executedAt")
    void returnCanceledOrderWithoutExecutedAt() throws Exception {
        mockMvc.perform(get("/orders/" + CANCELED_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(CANCELED_ORDER_ID))
            .andExpect(jsonPath("$.ticker").value("GOOG"))
            .andExpect(jsonPath("$.side").value("BUY"))
            .andExpect(jsonPath("$.status").value("CANCELED"))
            .andExpect(jsonPath("$.executedAt").value(nullValue()));
    }

    @Test
    @DisplayName("Should correctly calculate pricePerUnit from totalPrice and quantity")
    void correctlyCalculatesPricePerUnit() throws Exception {
        mockMvc.perform(get("/orders/" + FILLED_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPrice").value(1500.0))
            .andExpect(jsonPath("$.quantity").value(10))
            .andExpect(jsonPath("$.pricePerUnit").value(150.0));
    }

    @Test
    @DisplayName("Should populate ticker from resolved instrument")
    void populatesTicker() throws Exception {
        mockMvc.perform(get("/orders/" + FILLED_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value("GOOG"))
            .andExpect(jsonPath("$.ticker").isString());
    }

    @Test
    @DisplayName("Should not include nested instrument object")
    void responseDoesNotIncludeNestedInstrument() throws Exception {
        mockMvc.perform(get("/orders/" + FILLED_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").exists())
            .andExpect(jsonPath("$.instrument").doesNotExist())
            .andExpect(jsonPath("$.instrument.ticker").doesNotExist());
    }

    @Test
    @DisplayName("Should not include nested account object")
    void responseDoesNotIncludeNestedAccount() throws Exception {
        mockMvc.perform(get("/orders/" + FILLED_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account").doesNotExist())
            .andExpect(jsonPath("$.account.accountId").doesNotExist());
    }

    @Test
    @DisplayName("Should return 404 for nonexistent order")
    void returnNotFoundForNonexistentOrder() throws Exception {
        mockMvc.perform(get("/orders/" + NONEXISTENT_ORDER_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should include all required DTO fields")
    void includesAllRequiredFields() throws Exception {
        mockMvc.perform(get("/orders/" + FILLED_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").exists())
            .andExpect(jsonPath("$.ticker").exists())
            .andExpect(jsonPath("$.side").exists())
            .andExpect(jsonPath("$.quantity").exists())
            .andExpect(jsonPath("$.pricePerUnit").exists())
            .andExpect(jsonPath("$.totalPrice").exists())
            .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("Should have correct field types")
    void correctFieldTypes() throws Exception {
        mockMvc.perform(get("/orders/" + FILLED_ORDER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").isNumber())
            .andExpect(jsonPath("$.ticker").isString())
            .andExpect(jsonPath("$.side").isString())
            .andExpect(jsonPath("$.quantity").isNumber())
            .andExpect(jsonPath("$.pricePerUnit").isNumber())
            .andExpect(jsonPath("$.totalPrice").isNumber())
            .andExpect(jsonPath("$.status").isString());
    }
}
