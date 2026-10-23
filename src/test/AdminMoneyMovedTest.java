import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import test.config.TestSecurityConfig;

/**
 * GET /admin/analytics/money-moved
 *
 * The test clock is 2024-09-24 12:00 Eastern, so with days=30 the window starts 2024-08-26 00:00.
 * Orders are inserted directly and read back through the endpoint, so the real SQL is exercised.
 */
@SpringBootTest(classes = Application.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
@Transactional
@DisplayName("Admin Money Moved Analytics Tests")
public class AdminMoneyMovedTest {

    private static final int USER_ID = 900801;
    private static final int ACCOUNT_ID = 900801;
    private static final int INSTRUMENT_ID = 900801;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int nextOrderId = 900801;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Money Moved User', 'money.moved@example.com', DATE '1990-01-01', '1 Test St', 'SSN_HASH_MM', 'PASS_HASH')",
            USER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, CAST('10000.0000' AS NUMERIC(18,4)), 'BALANCED', 'Stocks', now(), true)",
            ACCOUNT_ID, USER_ID);
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'MM_ONE', 'STOCK', 'Money Moved Co', 'USD')",
            INSTRUMENT_ID);

        // 2024-09-24 (today): two buys and a sell
        order("BUY", "FILLED", "1000.0000", "2024-09-24 09:31:00");
        order("BUY", "FILLED", "250.5000", "2024-09-24 11:15:00");
        order("SELL", "FILLED", "400.0000", "2024-09-24 10:00:00");
        // 2024-09-20: one sell only
        order("SELL", "FILLED", "730.2500", "2024-09-20 15:59:00");
        // 2024-08-26 00:00: the first instant inside a 30-day window
        order("BUY", "FILLED", "100.0000", "2024-08-26 00:00:00");
        // 2024-08-25 23:59: one minute before the window, must be left out
        order("BUY", "FILLED", "9999.0000", "2024-08-25 23:59:00");
        // Not filled: must never count
        order("BUY", "PENDING", "5000.0000", "2024-09-23 10:00:00");
        order("SELL", "CANCELED", "6000.0000", "2024-09-23 11:00:00");
        order("BUY", "DECLINED", "7000.0000", "2024-09-22 12:00:00");
        order("BUY", "FAILED", "8000.0000", "2024-09-21 13:00:00");
    }

    // updated_at is the fill time; created_at is set earlier, as real orders are placed before they fill
    private void order(String side, String status, String totalPrice, String updatedAt) {
        int orderId = nextOrderId++;
        jdbcTemplate.update(
            "INSERT INTO orders (order_id, side, account_id, instrument_id, status, quantity, total_price, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, 1, CAST(? AS NUMERIC(18,4)), CAST(? AS TIMESTAMP) - INTERVAL '1' HOUR, CAST(? AS TIMESTAMP))",
            orderId, side, ACCOUNT_ID, INSTRUMENT_ID, status, totalPrice, updatedAt, updatedAt);
    }

    @Test
    @DisplayName("Sums filled orders per fill day, split by side, oldest day first")
    void sumsFilledOrdersPerDayBySide() throws Exception {
        mockMvc.perform(get("/admin/analytics/money-moved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].date", contains("2024-08-26", "2024-09-20", "2024-09-24")))
            .andExpect(jsonPath("$[2].buyTotal").value(1250.5))
            .andExpect(jsonPath("$[2].sellTotal").value(400.0))
            .andExpect(jsonPath("$[2].orders").value(3))
            .andExpect(jsonPath("$[1].buyTotal").value(0))
            .andExpect(jsonPath("$[1].sellTotal").value(730.25))
            .andExpect(jsonPath("$[1].orders").value(1))
            .andExpect(jsonPath("$[0].buyTotal").value(100.0))
            .andExpect(jsonPath("$[0].orders").value(1));
    }

    @Test
    @DisplayName("Leaves out orders that were not filled, and fills from before the window")
    void ignoresUnfilledOrdersAndOlderFills() throws Exception {
        mockMvc.perform(get("/admin/analytics/money-moved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].date", not(hasItem("2024-08-25"))))
            .andExpect(jsonPath("$[*].date", not(hasItem("2024-09-23"))))
            .andExpect(jsonPath("$[*].date", not(hasItem("2024-09-22"))))
            .andExpect(jsonPath("$[*].date", not(hasItem("2024-09-21"))));
    }

    @Test
    @DisplayName("days narrows the window, counting today as day one")
    void daysParameterNarrowsTheWindow() throws Exception {
        // days=5: 2024-09-20 through 2024-09-24
        mockMvc.perform(get("/admin/analytics/money-moved").param("days", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].date", contains("2024-09-20", "2024-09-24")));
        // days=1: today only
        mockMvc.perform(get("/admin/analytics/money-moved").param("days", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].date", contains("2024-09-24")));
    }

    @Test
    @DisplayName("Rejects days outside 1 to 365 with a 400")
    void rejectsOutOfRangeDays() throws Exception {
        for (String days : new String[] {"0", "-3", "366"}) {
            mockMvc.perform(get("/admin/analytics/money-moved").param("days", days))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("days must be between 1 and 365"));
        }
    }

    @Test
    @DisplayName("Returns an empty list when nothing was filled in the window")
    void returnsEmptyListWithNoFills() throws Exception {
        jdbcTemplate.update("DELETE FROM orders WHERE account_id = ?", ACCOUNT_ID);

        mockMvc.perform(get("/admin/analytics/money-moved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
