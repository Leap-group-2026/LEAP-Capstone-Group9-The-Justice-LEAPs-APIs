import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import main.Application;
import test.config.TestClockConfig;
import test.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * POST /transactions/account/{accountId}/deposit and /withdraw, verified end to end.
 *
 * Fixtures are inserted with raw SQL through JdbcTemplate, and every write is checked by reading the
 * accounts and transactions tables directly, so a response that claims success without moving money
 * (or a rejection that moved it anyway) fails here. Roles are covered through the real chain in SecurityTest.
 */
@SpringBootTest(classes = Application.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
@Transactional
public class FundsTransferTest {

    private static final int USER_ID = 940101;
    private static final int OTHER_USER_ID = 940102;
    private static final int ACCOUNT_ID = 940101;
    private static final int OTHER_ACCOUNT_ID = 940102;    // belongs to OTHER_USER_ID
    private static final int CLOSED_ACCOUNT_ID = 940103;   // belongs to USER_ID, closed
    private static final int NEVER_EXISTED_ACCOUNT_ID = 999996;

    // TestClockConfig is fixed at 2024-09-24 16:00 UTC; rows must carry Eastern time like order fills
    private static final String EASTERN_NOW = "2024-09-24T12:00:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        for (int userId : new int[] {USER_ID, OTHER_USER_ID}) {
            jdbcTemplate.update(
                "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
                "VALUES (?, 'Funds Test', ?, DATE '1990-01-01', '1 Test St', ?, 'pass')",
                userId, userId + "@funds.test", "ssn-" + userId);
        }
        insertAccount(ACCOUNT_ID, USER_ID, "1000.0000", true);
        insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "1000.0000", true);
        insertAccount(CLOSED_ACCOUNT_ID, USER_ID, "500.0000", false);
    }

    private void insertAccount(int accountId, int userId, String balance, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, CAST(? AS NUMERIC(18,4)), 'BALANCED', 'Passive', now(), ?)",
            accountId, userId, balance, active);
    }

    private ResultActions send(String action, int accountId, String body) throws Exception {
        return send(action, accountId, body, "CLIENT");
    }

    private ResultActions send(String action, int accountId, String body, String role) throws Exception {
        return mockMvc.perform(post("/transactions/account/" + accountId + "/" + action)
            .with(user(String.valueOf(USER_ID)).roles(role))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private BigDecimal balance(int accountId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, accountId);
    }

    private int transactionCount(int accountId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM transactions WHERE account_id = ?", Integer.class, accountId);
    }

    private void assertBalance(String expected, int accountId) {
        BigDecimal actual = balance(accountId);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "balance of " + accountId + " should be " + expected + ", was " + actual);
    }

    private void assertNothingMoved(int accountId, String expectedBalance) {
        assertBalance(expectedBalance, accountId);
        assertEquals(0, transactionCount(accountId), "a transaction row was written for a rejected request");
    }

    // ============ Happy paths ============

    @Test
    void depositAddsToTheBalanceAndRecordsAnInRow() throws Exception {
        send("deposit", ACCOUNT_ID, "{\"amount\": 250.00, \"fundingMethod\": \"bank\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.*", hasSize(2)))
            .andExpect(jsonPath("$.transaction.*", hasSize(7)))
            .andExpect(jsonPath("$.transaction.transaction_id").isNumber())
            .andExpect(jsonPath("$.transaction.transaction_type").value("DEPOSIT"))
            .andExpect(jsonPath("$.transaction.amount").value(250.0))
            .andExpect(jsonPath("$.transaction.side").value("IN"))
            .andExpect(jsonPath("$.transaction.account_id").value(ACCOUNT_ID))
            .andExpect(jsonPath("$.transaction.happened_at").value(EASTERN_NOW))
            .andExpect(jsonPath("$.balance").value(1250.0));

        assertBalance("1250", ACCOUNT_ID);
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT amount, side, transaction_type, happened_at FROM transactions WHERE account_id = ?", ACCOUNT_ID);
        assertEquals(0, new BigDecimal("250").compareTo((BigDecimal) row.get("AMOUNT")));
        assertEquals("IN", row.get("SIDE"));
        assertEquals("DEPOSIT", row.get("TRANSACTION_TYPE"));
        assertEquals(LocalDateTime.parse(EASTERN_NOW), ((Timestamp) row.get("HAPPENED_AT")).toLocalDateTime());
    }

    @Test
    void withdrawSubtractsFromTheBalanceAndRecordsAnOutRow() throws Exception {
        send("withdraw", ACCOUNT_ID, "{\"amount\": 100.50}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transaction.transaction_type").value("WITHDRAWAL"))
            .andExpect(jsonPath("$.transaction.amount").value(100.5))
            .andExpect(jsonPath("$.transaction.side").value("OUT"))
            .andExpect(jsonPath("$.transaction.account_id").value(ACCOUNT_ID))
            .andExpect(jsonPath("$.transaction.happened_at").value(EASTERN_NOW))
            .andExpect(jsonPath("$.balance").value(899.5));

        assertBalance("899.50", ACCOUNT_ID);
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM transactions WHERE account_id = ? AND side = 'OUT' AND transaction_type = 'WITHDRAWAL' AND amount = 100.5",
            Integer.class, ACCOUNT_ID));
    }

    @Test
    void withdrawingTheWholeBalanceLeavesZero() throws Exception {
        send("withdraw", ACCOUNT_ID, "{\"amount\": 1000}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(0.0));

        assertBalance("0", ACCOUNT_ID);
    }

    @Test
    void theNewRowIsFirstInTheAccountsHistory() throws Exception {
        jdbcTemplate.update(
            "INSERT INTO transactions (amount, side, account_id, transaction_type, happened_at) " +
            "VALUES (5.0000, 'IN', ?, 'DEPOSIT', TIMESTAMP '2024-01-01 09:00:00')", ACCOUNT_ID);

        String body = send("deposit", ACCOUNT_ID, "{\"amount\": 10}")
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        int newId = com.jayway.jsonpath.JsonPath.read(body, "$.transaction.transaction_id");

        mockMvc.perform(get("/transactions/account/" + ACCOUNT_ID).with(user(String.valueOf(USER_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].transaction_id").value(newId))
            .andExpect(jsonPath("$[0].happened_at").value(EASTERN_NOW));
    }

    @Test
    void balanceAlwaysMatchesTheOpeningBalancePlusTheLedger() throws Exception {
        send("deposit", ACCOUNT_ID, "{\"amount\": 250}").andExpect(status().isOk());
        send("withdraw", ACCOUNT_ID, "{\"amount\": 100.50}").andExpect(status().isOk());
        send("deposit", ACCOUNT_ID, "{\"amount\": 0.25}").andExpect(status().isOk());
        send("withdraw", ACCOUNT_ID, "{\"amount\": 5000}").andExpect(status().isBadRequest());
        send("withdraw", ACCOUNT_ID, "{\"amount\": 0}").andExpect(status().isBadRequest());

        BigDecimal ledger = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(CASE side WHEN 'IN' THEN amount ELSE -amount END), 0) FROM transactions WHERE account_id = ?",
            BigDecimal.class, ACCOUNT_ID);
        assertEquals(0, new BigDecimal("1000").add(ledger).compareTo(balance(ACCOUNT_ID)),
            "balance " + balance(ACCOUNT_ID) + " does not equal 1000 + ledger " + ledger);
        assertEquals(3, transactionCount(ACCOUNT_ID));
        assertBalance("1149.75", ACCOUNT_ID);
    }

    // ============ fundingMethod ============

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"amount\": 10}",
        "{\"amount\": 10, \"fundingMethod\": null}",
        "{\"amount\": 10, \"fundingMethod\": \"bank\"}",
        "{\"amount\": 10, \"fundingMethod\": \"debit\"}",
        "{\"amount\": 10, \"fundingMethod\": \"Bank\"}",
        "{\"amount\": 10, \"fundingMethod\": \"DEBIT\"}"
    })
    void fundingMethodIsOptionalAndAnyCasing(String body) throws Exception {
        send("deposit", ACCOUNT_ID, body).andExpect(status().isOk());
        assertBalance("1010", ACCOUNT_ID);
    }

    @Test
    void anUnknownFundingMethodIsRejected() throws Exception {
        send("deposit", ACCOUNT_ID, "{\"amount\": 10, \"fundingMethod\": \"paypal\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldName").value("fundingMethod"));

        assertNothingMoved(ACCOUNT_ID, "1000");
    }

    // ============ Insufficient funds ============

    @Test
    void withdrawingMoreThanTheBalanceIsRejectedOnTheBalanceField() throws Exception {
        send("withdraw", ACCOUNT_ID, "{\"amount\": 1000.01}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.*", hasSize(6)))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.fieldName").value("balance"))
            .andExpect(jsonPath("$.rejectedValue").value(1000.01))
            .andExpect(jsonPath("$.fieldMessage").value(containsString("Insufficient balance")));

        assertNothingMoved(ACCOUNT_ID, "1000");
    }

    // ============ Accounts the caller may not use ============

    @ParameterizedTest
    @CsvSource({"deposit", "withdraw"})
    void anotherUsersAccountIsNotFoundAndUntouched(String action) throws Exception {
        send(action, OTHER_ACCOUNT_ID, "{\"amount\": 10}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.fieldName").value("Account"))
            .andExpect(jsonPath("$.rejectedValue").value(String.valueOf(OTHER_ACCOUNT_ID)));

        assertNothingMoved(OTHER_ACCOUNT_ID, "1000");
    }

    @ParameterizedTest
    @CsvSource({"deposit", "withdraw"})
    void aClosedAccountIsNotFoundAndUntouched(String action) throws Exception {
        send(action, CLOSED_ACCOUNT_ID, "{\"amount\": 10}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.fieldName").value("Account"));

        assertNothingMoved(CLOSED_ACCOUNT_ID, "500");
    }

    @ParameterizedTest
    @CsvSource({"deposit", "withdraw"})
    void anAccountThatNeverExistedIsNotFound(String action) throws Exception {
        send(action, NEVER_EXISTED_ACCOUNT_ID, "{\"amount\": 10}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.fieldName").value("Account"));
    }

    // The security rule stops admins first; this checks the controller's own guard behind it
    @ParameterizedTest
    @CsvSource({"deposit", "withdraw"})
    void aNonClientIsForbidden(String action) throws Exception {
        send(action, ACCOUNT_ID, "{\"amount\": 10}", "ADMIN")
            .andExpect(status().isForbidden());

        assertNothingMoved(ACCOUNT_ID, "1000");
    }

    // ============ Invalid amounts ============

    // Only fieldName is asserted: a value breaking two rules reports whichever the validator lists first
    @ParameterizedTest
    @CsvSource({
        "deposit, 0", "withdraw, 0",
        "deposit, -5", "withdraw, -5",
        "deposit, 10.123", "withdraw, 10.123",
        "deposit, 100000.01", "withdraw, 100000.01",
        "deposit, 1000000", "withdraw, -0.001",
        "deposit, \"abc\"", "withdraw, \"abc\"",
        "deposit, null", "withdraw, null"
    })
    void invalidAmountsAreRejectedOnTheAmountField(String action, String amount) throws Exception {
        send(action, ACCOUNT_ID, "{\"amount\": " + amount + "}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldName").value("amount"));

        assertNothingMoved(ACCOUNT_ID, "1000");
    }

    @Test
    void aMissingAmountIsRejected() throws Exception {
        send("deposit", ACCOUNT_ID, "{\"fundingMethod\": \"bank\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldName").value("amount"))
            .andExpect(jsonPath("$.fieldMessage").value("amount is required"));

        assertNothingMoved(ACCOUNT_ID, "1000");
    }

    @Test
    void threeDecimalPlacesNamesTheRule() throws Exception {
        send("deposit", ACCOUNT_ID, "{\"amount\": 10.123}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.rejectedValue").value(10.123))
            .andExpect(jsonPath("$.fieldMessage").value("amount must have at most 2 decimal places"));
    }

    @Test
    void overTheLimitNamesTheRule() throws Exception {
        send("deposit", ACCOUNT_ID, "{\"amount\": 100000.01}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldMessage").value("amount must be at most 100000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "100000", "100000.00", "1.5"})
    void boundaryAmountsAreAccepted(String amount) throws Exception {
        send("deposit", ACCOUNT_ID, "{\"amount\": " + amount + "}").andExpect(status().isOk());
        assertBalance(new BigDecimal("1000").add(new BigDecimal(amount)).toPlainString(), ACCOUNT_ID);
    }
}
