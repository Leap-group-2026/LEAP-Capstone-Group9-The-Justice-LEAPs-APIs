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
 * Admin endpoints for account retrieval: GET /admin/accounts and GET /admin/accounts/{userId}
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("Admin Accounts Endpoints Tests")
public class AdminAccountsTest {

    private static final int USER_1_ID = 900601;
    private static final int USER_2_ID = 900602;
    private static final int NONEXISTENT_USER_ID = 999999;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Create test users
        insertUser(USER_1_ID, "Admin Test User 1", "admin.user1@example.com");
        insertUser(USER_2_ID, "Admin Test User 2", "admin.user2@example.com");

        // Create multiple accounts for USER_1
        insertAccount(900601, USER_1_ID, "1000.0000", "BALANCED", "Stocks", true);
        insertAccount(900602, USER_1_ID, "5000.0000", "HIGH", "Options", true);

        // Create accounts for USER_2
        insertAccount(900603, USER_2_ID, "2000.0000", "LOW", "Stocks", true);
    }

    private void insertUser(int userId, String name, String email) {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, ?, ?, DATE '1990-01-01', '123 Test St', ?, 'PASS_HASH')",
            userId, name, email, "SSN_HASH_" + userId);
    }

    private void insertAccount(int accountId, int userId, String balance, String portfolioSize, 
                               String tradeType, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, CAST(? AS NUMERIC(18,4)), ?, ?, now(), ?)",
            accountId, userId, balance, portfolioSize, tradeType, active);
    }

    @Test
    @DisplayName("GET /admin/accounts - Should return all accounts across all users")
    void getAllAccountsReturnsAllAccounts() throws Exception {
        mockMvc.perform(get("/admin/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].userId", containsInAnyOrder(USER_1_ID, USER_1_ID, USER_2_ID)))
            .andExpect(jsonPath("$[*].accountId", containsInAnyOrder(900601, 900602, 900603)));
    }

    @Test
    @DisplayName("GET /admin/accounts/{userId} - Should return only that user's accounts")
    void getUserAccountsReturnsOnlySpecificUserAccounts() throws Exception {
        mockMvc.perform(get("/admin/accounts/" + USER_1_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].userId").value(USER_1_ID))
            .andExpect(jsonPath("$[1].userId").value(USER_1_ID));
    }

    @Test
    @DisplayName("GET /admin/accounts/{userId} - Should return empty list for user with no accounts")
    void getUserAccountsReturnsEmptyListForNonexistentUser() throws Exception {
        mockMvc.perform(get("/admin/accounts/" + NONEXISTENT_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
