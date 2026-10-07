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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import main.Application;
import test.config.TestClockConfig;
import test.config.TestSecurityConfig;

/**
 * GET /transactions/account/{accountId}, verified end to end.
 *
 * Fixtures are inserted with raw SQL through JdbcTemplate, never through the code under test,
 * so every assertion checks a value that was written independently of the query that reads it.
 */
@SpringBootTest(classes = Application.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
@Transactional
public class TransactionHistoryByAccountTest {

    private static final int USER_ID = 900301;
    private static final int ACCOUNT_ID = 900301;
    private static final int OTHER_ACCOUNT_ID = 900302;
    private static final int CLOSED_ACCOUNT_ID = 900303;
    private static final int EMPTY_ACCOUNT_ID = 900304;
    private static final int NEVER_EXISTED_ACCOUNT_ID = 999997;

    private static final int DEPOSIT_ID = 900401;
    private static final int EXCHANGE_ID = 900402;
    private static final int WITHDRAWAL_ID = 900403;
    private static final int OTHER_ACCOUNT_TRANSACTION_ID = 900404;
    private static final int CLOSED_ACCOUNT_TRANSACTION_ID = 900405;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Txn Test', 'txn.test@example.com', DATE '1990-01-01', '1 Test St', 'SSN_HASH_SENTINEL', 'PASS_HASH_SENTINEL')",
            USER_ID);
        insertAccount(ACCOUNT_ID, true);
        insertAccount(OTHER_ACCOUNT_ID, true);
        insertAccount(CLOSED_ACCOUNT_ID, false);
        insertAccount(EMPTY_ACCOUNT_ID, true);

        // Inserted out of time order, so a missing ORDER BY is likely to show
        insertTransaction(EXCHANGE_ID, ACCOUNT_ID, "30.5000", "EXCHANGE", "CURRENCY EXCHANGE", "2026-01-02 11:00:00");
        insertTransaction(DEPOSIT_ID, ACCOUNT_ID, "1000.0000", "IN", "DEPOSIT", "2026-01-01 09:00:00");
        insertTransaction(WITHDRAWAL_ID, ACCOUNT_ID, "200.2500", "OUT", "WITHDRAWAL", "2026-01-03 15:30:00");
        insertTransaction(OTHER_ACCOUNT_TRANSACTION_ID, OTHER_ACCOUNT_ID, "50.0000", "IN", "DEPOSIT", "2026-01-04 09:00:00");
        insertTransaction(CLOSED_ACCOUNT_TRANSACTION_ID, CLOSED_ACCOUNT_ID, "50.0000", "IN", "DEPOSIT", "2026-01-04 09:00:00");
    }

    private void insertAccount(int accountId, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 1000.0000, 'BALANCED', 'Passive', now(), ?)",
            accountId, USER_ID, active);
    }

    private void insertTransaction(int transactionId, int accountId, String amount, String side, String type, String happenedAt) {
        jdbcTemplate.update(
            "INSERT INTO transactions (transaction_id, amount, side, account_id, transaction_type, happened_at) " +
            "VALUES (?, CAST(? AS NUMERIC(18,4)), ?, ?, ?, CAST(? AS TIMESTAMP))",
            transactionId, amount, side, accountId, type, happenedAt);
    }

    @Test
    void transactionReturnsEveryAcceptanceCriteriaField() throws Exception {
        mockMvc.perform(get("/transactions/account/" + ACCOUNT_ID)
            .with(user("" + USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].transaction_id").value(EXCHANGE_ID))
            .andExpect(jsonPath("$[1].transaction_type").value("CURRENCY EXCHANGE"))
            .andExpect(jsonPath("$[1].amount").value(30.5))
            .andExpect(jsonPath("$[1].side").value("EXCHANGE"))
            .andExpect(jsonPath("$[1].account_id").value(ACCOUNT_ID))
            .andExpect(jsonPath("$[1].happened_at").value("2026-01-02T11:00:00"));
    }

    @Test
    void responseContainsOnlyTheStoryFields() throws Exception {
        mockMvc.perform(get("/transactions/account/" + ACCOUNT_ID)
            .with(user("" + USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].*", hasSize(6)));
    }

    @Test
    void returnsOnlyThisAccountsTransactionsNewestFirst() throws Exception {
        mockMvc.perform(get("/transactions/account/" + ACCOUNT_ID)
            .with(user("" + USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].transaction_id", contains(WITHDRAWAL_ID, EXCHANGE_ID, DEPOSIT_ID)))
            .andExpect(jsonPath("$[*].account_id", everyItem(is(ACCOUNT_ID))));
    }

    @Test
    void accountWithNoTransactionsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/transactions/account/" + EMPTY_ACCOUNT_ID)
            .with(user("" + USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(get("/transactions/account/" + NEVER_EXISTED_ACCOUNT_ID)
            .with(user("" + USER_ID)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void closedAccountIsNotFoundEvenWithTransactions() throws Exception {
        mockMvc.perform(get("/transactions/account/" + CLOSED_ACCOUNT_ID)
            .with(user("" + USER_ID)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}
