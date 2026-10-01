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
 * GET /accounts/user/{userId}, verified end to end.
 * 
 * Tests the simplified AccountResponse DTO which removes nested user objects.
 * Fixtures are inserted with raw SQL through JdbcTemplate, never through the code under test,
 * so every assertion checks a value that was written independently of the query that reads it.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("GET /accounts/user/{userId} Tests")
public class GetAccountsByUserIdTest {

    private static final int USER_ID = 900501;
    private static final int OTHER_USER_ID = 900502;
    private static final int NONEXISTENT_USER_ID = 999999;
    private static final int ACTIVE_ACCOUNT_ID = 900501;
    private static final int CLOSED_ACCOUNT_ID = 900502;
    private static final int OTHER_USER_ACCOUNT_ID = 900503;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Create test users
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Test User One', 'user1@example.com', DATE '1990-01-01', '1 Test St', 'SSN_HASH_1', 'PASS_HASH_1')",
            USER_ID);
        
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Test User Two', 'user2@example.com', DATE '1991-02-02', '2 Test St', 'SSN_HASH_2', 'PASS_HASH_2')",
            OTHER_USER_ID);

        // Create accounts for USER_ID
        insertAccount(ACTIVE_ACCOUNT_ID, USER_ID, "1000.0000", "BALANCED", "Passive", true);
        insertAccount(CLOSED_ACCOUNT_ID, USER_ID, "5000.0000", "HIGH", "Active", false);
        
        // Create account for OTHER_USER_ID (should not be returned)
        insertAccount(OTHER_USER_ACCOUNT_ID, OTHER_USER_ID, "2000.0000", "LOW", "Passive", true);
    }

    private void insertAccount(int accountId, int userId, String balance, String portfolioSize, 
                               String tradeType, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, CAST(? AS NUMERIC(18,4)), ?, ?, now(), ?)",
            accountId, userId, balance, portfolioSize, tradeType, active);
    }

    @Test
    @DisplayName("Should return only active accounts for the user")
    void returnsOnlyActiveAccountsForUser() throws Exception {
        mockMvc.perform(get("/accounts/user/" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].accountId").value(ACTIVE_ACCOUNT_ID))
            .andExpect(jsonPath("$[0].userId").value(USER_ID))
            .andExpect(jsonPath("$[0].balance").value(1000.0))
            .andExpect(jsonPath("$[0].portfolioSize").value("BALANCED"))
            .andExpect(jsonPath("$[0].tradeType").value("Passive"))
            .andExpect(jsonPath("$[0].accountActive").value(true));
    }

    @Test
    @DisplayName("Should return all active accounts for user with multiple accounts")
    void returnsAllActiveAccountsForUserWithMultiple() throws Exception {
        // Add another active account for USER_ID
        insertAccount(900504, USER_ID, "3000.0000", "LOW", "Conservative", true);
        
        mockMvc.perform(get("/accounts/user/" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].accountId", containsInAnyOrder(ACTIVE_ACCOUNT_ID, 900504)))
            .andExpect(jsonPath("$[*].userId", everyItem(equalTo(USER_ID))))
            .andExpect(jsonPath("$[*].accountActive", everyItem(equalTo(true))));
    }

    @Test
    @DisplayName("Should not include nested user objects in response")
    void responseDoesNotIncludeNestedUserObject() throws Exception {
        mockMvc.perform(get("/accounts/user/" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").exists())
            // Verify fields that should exist
            .andExpect(jsonPath("$[0].accountId").exists())
            .andExpect(jsonPath("$[0].userId").exists())
            .andExpect(jsonPath("$[0].balance").exists())
            // Verify nested user object does NOT exist
            .andExpect(jsonPath("$[0].user").doesNotExist())
            .andExpect(jsonPath("$[0].userId.name").doesNotExist());
    }

    @Test
    @DisplayName("Should return empty list when user has no active accounts")
    void returnsEmptyListWhenNoActiveAccounts() throws Exception {
        // Create a new user with no accounts
        int newUserId = 900505;
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Empty User', 'empty@example.com', DATE '1992-03-03', '3 Test St', 'SSN_HASH_3', 'PASS_HASH_3')",
            newUserId);
        
        mockMvc.perform(get("/accounts/user/" + newUserId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return empty list for nonexistent user")
    void returnsEmptyListForNonexistentUser() throws Exception {
        mockMvc.perform(get("/accounts/user/" + NONEXISTENT_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return correct DTO fields in response")
    void responseIncludesAllDtoFields() throws Exception {
        mockMvc.perform(get("/accounts/user/" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].accountId").isNumber())
            .andExpect(jsonPath("$[0].userId").isNumber())
            .andExpect(jsonPath("$[0].balance").isNumber())
            .andExpect(jsonPath("$[0].portfolioSize").isString())
            .andExpect(jsonPath("$[0].tradeType").isString())
            .andExpect(jsonPath("$[0].createdAt").exists())
            .andExpect(jsonPath("$[0].accountActive").isBoolean());
    }

    @Test
    @DisplayName("Should not return closed accounts even if they exist")
    void doesNotReturnClosedAccounts() throws Exception {
        mockMvc.perform(get("/accounts/user/" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[*].accountId", not(hasItem(CLOSED_ACCOUNT_ID))));
    }

    @Test
    @DisplayName("Should return only accounts belonging to the specified user")
    void returnsOnlyAccountsForSpecifiedUser() throws Exception {
        mockMvc.perform(get("/accounts/user/" + USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[*].userId", everyItem(equalTo(USER_ID))))
            .andExpect(jsonPath("$[*].accountId", not(hasItem(OTHER_USER_ACCOUNT_ID))));
    }
}
