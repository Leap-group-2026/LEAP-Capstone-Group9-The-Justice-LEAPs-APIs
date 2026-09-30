package test;

import org.junit.jupiter.api.BeforeEach;
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

/**
 * GET /orders/account/{accountId}, verified end to end.
 *
 * Fixtures are inserted with raw SQL through JdbcTemplate, never through the code under test,
 * so every assertion checks a value that was written independently of the query that reads it.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
public class OrderHistoryByAccountTest {

    private static final int USER_ID = 900101;
    private static final int ACCOUNT_ID = 900101;
    private static final int OTHER_ACCOUNT_ID = 900102;
    private static final int CLOSED_ACCOUNT_ID = 900103;
    private static final int EMPTY_ACCOUNT_ID = 900104;
    private static final int NEVER_EXISTED_ACCOUNT_ID = 999998;
    private static final int ALPHA_INSTRUMENT_ID = 900101;
    private static final int BETA_INSTRUMENT_ID = 900102;

    private static final int FILLED_ORDER_ID = 900201;
    private static final int CANCELED_ORDER_ID = 900202;
    private static final int OTHER_ACCOUNT_ORDER_ID = 900203;
    private static final int CLOSED_ACCOUNT_ORDER_ID = 900204;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'History Test', 'history.test@example.com', DATE '1990-01-01', '1 Test St', 'SSN_HASH_SENTINEL', 'PASS_HASH_SENTINEL')",
            USER_ID);
        insertAccount(ACCOUNT_ID, true);
        insertAccount(OTHER_ACCOUNT_ID, true);
        insertAccount(CLOSED_ACCOUNT_ID, false);
        insertAccount(EMPTY_ACCOUNT_ID, true);

        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'OH_ALPHA', 'STOCK', 'Alpha Co', 'USD')",
            ALPHA_INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'OH_BETA', 'STOCK', 'Beta Co', 'USD')",
            BETA_INSTRUMENT_ID);

        // Older order, executed: 30 / 3 = 10 per unit
        insertOrder(FILLED_ORDER_ID, ACCOUNT_ID, ALPHA_INSTRUMENT_ID, "BUY", "FILLED", 3, "30.0000",
            "2026-01-01 10:00:00", "2026-01-01 10:05:00");
        // Newer order, never executed: 10 / 3 does not terminate, so the scale must be pinned
        insertOrder(CANCELED_ORDER_ID, ACCOUNT_ID, BETA_INSTRUMENT_ID, "SELL", "CANCELED", 3, "10.0000",
            "2026-01-02 09:00:00", "2026-01-02 09:30:00");
        insertOrder(OTHER_ACCOUNT_ORDER_ID, OTHER_ACCOUNT_ID, ALPHA_INSTRUMENT_ID, "BUY", "FILLED", 1, "10.0000",
            "2026-01-03 09:00:00", "2026-01-03 09:01:00");
        insertOrder(CLOSED_ACCOUNT_ORDER_ID, CLOSED_ACCOUNT_ID, ALPHA_INSTRUMENT_ID, "BUY", "FILLED", 1, "10.0000",
            "2026-01-03 09:00:00", "2026-01-03 09:01:00");
    }

    private void insertAccount(int accountId, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 1000.0000, 'BALANCED', 'Passive', now(), ?)",
            accountId, USER_ID, active);
    }

    private void insertOrder(int orderId, int accountId, int instrumentId, String side, String status,
                             int quantity, String totalPrice, String createdAt, String updatedAt) {
        jdbcTemplate.update(
            "INSERT INTO orders (order_id, side, account_id, instrument_id, status, quantity, total_price, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS NUMERIC(18,4)), CAST(? AS TIMESTAMP), CAST(? AS TIMESTAMP))",
            orderId, side, accountId, instrumentId, status, quantity, totalPrice, createdAt, updatedAt);
    }

    @Test
    void filledOrderReturnsEveryAcceptanceCriteriaField() throws Exception {
        mockMvc.perform(get("/orders/account/" + ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].orderId").value(FILLED_ORDER_ID))
            .andExpect(jsonPath("$[1].ticker").value("OH_ALPHA"))
            .andExpect(jsonPath("$[1].side").value("BUY"))
            .andExpect(jsonPath("$[1].status").value("FILLED"))
            .andExpect(jsonPath("$[1].quantity").value(3))
            .andExpect(jsonPath("$[1].pricePerUnit").value(10.0))
            .andExpect(jsonPath("$[1].totalPrice").value(30.0))
            .andExpect(jsonPath("$[1].executedAt").value("2026-01-01T10:05:00"));
    }

    @Test
    void unexecutedOrderHasNoExecutionTimeAndPricePerUnitIsRounded() throws Exception {
        mockMvc.perform(get("/orders/account/" + ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderId").value(CANCELED_ORDER_ID))
            .andExpect(jsonPath("$[0].ticker").value("OH_BETA"))
            .andExpect(jsonPath("$[0].side").value("SELL"))
            .andExpect(jsonPath("$[0].status").value("CANCELED"))
            .andExpect(jsonPath("$[0].pricePerUnit").value(3.3333))
            .andExpect(jsonPath("$[0].totalPrice").value(10.0))
            .andExpect(jsonPath("$[0].executedAt").value(nullValue()));
    }

    @Test
    void returnsOnlyThisAccountsOrdersNewestFirst() throws Exception {
        mockMvc.perform(get("/orders/account/" + ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].orderId", contains(CANCELED_ORDER_ID, FILLED_ORDER_ID)));
    }

    @Test
    void accountWithNoOrdersReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/orders/account/" + EMPTY_ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(get("/orders/account/" + NEVER_EXISTED_ACCOUNT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void closedAccountIsNotFoundEvenWithOrders() throws Exception {
        mockMvc.perform(get("/orders/account/" + CLOSED_ACCOUNT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}
