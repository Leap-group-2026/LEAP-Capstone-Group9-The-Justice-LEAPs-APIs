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
import test.config.TestSecurityConfig;

/**
 * Admin endpoints for one user's orders: GET /admin/orders/{userId} and GET /admin/orders/{userId}/cancelled
 *
 * Fixtures are inserted directly with JdbcTemplate and read back through the endpoint, so the
 * test exercises the real SQL and result mapping rather than agreeing with itself.
 */
@SpringBootTest(classes = Application.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
@Transactional
@DisplayName("Admin User Orders Endpoint Tests")
public class AdminUserOrdersTest {

    private static final int USER_1_ID = 900701;
    private static final int USER_2_ID = 900702;
    private static final int USER_NO_ORDERS_ID = 900703;
    private static final int USER_4_ID = 900704;
    private static final int NONEXISTENT_USER_ID = 999999;

    private static final int USER_1_ACCOUNT_A = 900701;
    private static final int USER_1_ACCOUNT_B = 900702;
    private static final int USER_2_ACCOUNT = 900703;
    private static final int USER_NO_ORDERS_ACCOUNT = 900704;
    private static final int USER_4_ACCOUNT = 900705;

    private static final int INSTRUMENT_1_ID = 900701;
    private static final int INSTRUMENT_2_ID = 900702;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        insertUser(USER_1_ID, "Orders Test User 1", "orders.user1@example.com");
        insertUser(USER_2_ID, "Orders Test User 2", "orders.user2@example.com");
        insertUser(USER_NO_ORDERS_ID, "Orders Test User 3", "orders.user3@example.com");
        insertUser(USER_4_ID, "Orders Test User 4", "orders.user4@example.com");

        // USER_1 owns two accounts so the query must join through accounts, not match a single account
        insertAccount(USER_1_ACCOUNT_A, USER_1_ID);
        insertAccount(USER_1_ACCOUNT_B, USER_1_ID);
        insertAccount(USER_2_ACCOUNT, USER_2_ID);
        insertAccount(USER_NO_ORDERS_ACCOUNT, USER_NO_ORDERS_ID);
        insertAccount(USER_4_ACCOUNT, USER_4_ID);

        insertInstrument(INSTRUMENT_1_ID, "AUO_ONE");
        insertInstrument(INSTRUMENT_2_ID, "AUO_TWO");

        insertOrder(900701, "BUY", USER_1_ACCOUNT_A, INSTRUMENT_1_ID, "FILLED", 10, "1000.0000",
            "2026-09-01 10:00:00", "2026-09-01 10:05:00");
        insertOrder(900702, "SELL", USER_1_ACCOUNT_B, INSTRUMENT_2_ID, "PENDING", 5, "750.5000",
            "2026-09-02 11:00:00", "2026-09-02 11:00:00");
        insertOrder(900703, "BUY", USER_1_ACCOUNT_A, INSTRUMENT_2_ID, "CANCELED", 3, "300.0000",
            "2026-09-03 12:00:00", "2026-09-03 12:30:00");
        // Created before 900703 but cancelled after it, so newest-cancelled-first puts it ahead
        insertOrder(900705, "SELL", USER_1_ACCOUNT_B, INSTRUMENT_1_ID, "CANCELED", 7, "700.0000",
            "2026-09-01 09:00:00", "2026-09-05 09:00:00");

        // Belongs to USER_2 and must never appear in USER_1's results
        insertOrder(900704, "BUY", USER_2_ACCOUNT, INSTRUMENT_1_ID, "FILLED", 20, "3200.7500",
            "2026-09-04 13:00:00", "2026-09-04 13:01:00");

        // A cancelled order owned by someone else; must never appear in another user's cancelled list
        insertOrder(900706, "BUY", USER_4_ACCOUNT, INSTRUMENT_1_ID, "CANCELED", 1, "100.0000",
            "2026-09-06 08:00:00", "2026-09-06 08:10:00");
    }

    private void insertUser(int userId, String name, String email) {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, ?, ?, DATE '1990-01-01', '123 Test St', ?, 'PASS_HASH')",
            userId, name, email, "SSN_HASH_" + userId);
    }

    private void insertAccount(int accountId, int userId) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, CAST('10000.0000' AS NUMERIC(18,4)), 'BALANCED', 'Stocks', now(), true)",
            accountId, userId);
    }

    private void insertInstrument(int instrumentId, String ticker) {
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, ?, 'STOCK', 'Admin Orders Test Co', 'USD')",
            instrumentId, ticker);
    }

    private void insertOrder(int orderId, String side, int accountId, int instrumentId, String status,
                             int quantity, String totalPrice, String createdAt, String updatedAt) {
        jdbcTemplate.update(
            "INSERT INTO orders (order_id, side, account_id, instrument_id, status, quantity, total_price, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS NUMERIC(18,4)), CAST(? AS TIMESTAMP), CAST(? AS TIMESTAMP))",
            orderId, side, accountId, instrumentId, status, quantity, totalPrice, createdAt, updatedAt);
    }

    @Test
    @DisplayName("GET /admin/orders/{userId} - Should return orders from every account the user owns, and no one else's")
    void returnsOnlyThatUsersOrdersAcrossAllAccounts() throws Exception {
        mockMvc.perform(get("/admin/orders/" + USER_1_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[*].order_id", containsInAnyOrder(900701, 900702, 900703, 900705)))
            .andExpect(jsonPath("$[*].account_id",
                containsInAnyOrder(USER_1_ACCOUNT_A, USER_1_ACCOUNT_B, USER_1_ACCOUNT_A, USER_1_ACCOUNT_B)))
            .andExpect(jsonPath("$[*].order_id", not(hasItem(900704))))
            .andExpect(jsonPath("$[*].order_id", not(hasItem(900706))));
    }

    @Test
    @DisplayName("GET /admin/orders/{userId} - Should map every column to the value stored in the database")
    void mapsEveryFieldFromTheDatabaseRow() throws Exception {
        // Checks values, not just presence, so a broken result mapping that returns nulls fails here
        mockMvc.perform(get("/admin/orders/" + USER_2_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].order_id").value(900704))
            .andExpect(jsonPath("$[0].side").value("BUY"))
            .andExpect(jsonPath("$[0].account_id").value(USER_2_ACCOUNT))
            .andExpect(jsonPath("$[0].instrument_id").value(INSTRUMENT_1_ID))
            .andExpect(jsonPath("$[0].status").value("FILLED"))
            .andExpect(jsonPath("$[0].quantity").value(20))
            .andExpect(jsonPath("$[0].total_price").value(3200.75))
            .andExpect(jsonPath("$[0].created_at").value(startsWith("2026-09-04T13:00")))
            .andExpect(jsonPath("$[0].updated_at").value(startsWith("2026-09-04T13:01")));
    }

    @Test
    @DisplayName("GET /admin/orders/{userId} - Should return orders in every status, not just open ones")
    void includesOrdersInEveryStatus() throws Exception {
        mockMvc.perform(get("/admin/orders/" + USER_1_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].status", containsInAnyOrder("FILLED", "PENDING", "CANCELED", "CANCELED")));
    }

    @Test
    @DisplayName("GET /admin/orders/{userId} - Should return an empty list for a user whose accounts have no orders")
    void returnsEmptyListForUserWithNoOrders() throws Exception {
        mockMvc.perform(get("/admin/orders/" + USER_NO_ORDERS_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /admin/orders/{userId} - Should return an empty list for a user that does not exist")
    void returnsEmptyListForNonexistentUser() throws Exception {
        // Matches GET /admin/accounts/{userId}; change to a 404 here if the endpoint is made to validate the user
        mockMvc.perform(get("/admin/orders/" + NONEXISTENT_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /admin/orders/{userId}/cancelled - Should return only the user's cancelled orders, newest cancellation first")
    void cancelledReturnsOnlyThatUsersCancelledOrdersNewestFirst() throws Exception {
        // 900705 was created earlier but cancelled later than 900703, so this also pins the sort to updated_at
        mockMvc.perform(get("/admin/orders/" + USER_1_ID + "/cancelled"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].order_id", contains(900705, 900703)))
            .andExpect(jsonPath("$[*].status", everyItem(is("CANCELED"))))
            .andExpect(jsonPath("$[*].account_id", containsInAnyOrder(USER_1_ACCOUNT_A, USER_1_ACCOUNT_B)));
    }

    @Test
    @DisplayName("GET /admin/orders/{userId}/cancelled - Should map every column to the value stored in the database")
    void cancelledMapsEveryFieldFromTheDatabaseRow() throws Exception {
        mockMvc.perform(get("/admin/orders/" + USER_4_ID + "/cancelled"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].order_id").value(900706))
            .andExpect(jsonPath("$[0].side").value("BUY"))
            .andExpect(jsonPath("$[0].account_id").value(USER_4_ACCOUNT))
            .andExpect(jsonPath("$[0].instrument_id").value(INSTRUMENT_1_ID))
            .andExpect(jsonPath("$[0].status").value("CANCELED"))
            .andExpect(jsonPath("$[0].quantity").value(1))
            .andExpect(jsonPath("$[0].total_price").value(100.0))
            .andExpect(jsonPath("$[0].created_at").value(startsWith("2026-09-06T08:00")))
            .andExpect(jsonPath("$[0].updated_at").value(startsWith("2026-09-06T08:10")));
    }

    @Test
    @DisplayName("GET /admin/orders/{userId}/cancelled - Should return an empty list for a user with orders but none cancelled")
    void cancelledReturnsEmptyListWhenUserHasNoCancelledOrders() throws Exception {
        mockMvc.perform(get("/admin/orders/" + USER_2_ID + "/cancelled"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /admin/orders/{userId}/cancelled - Should return an empty list for a user that does not exist")
    void cancelledReturnsEmptyListForNonexistentUser() throws Exception {
        mockMvc.perform(get("/admin/orders/" + NONEXISTENT_USER_ID + "/cancelled"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
