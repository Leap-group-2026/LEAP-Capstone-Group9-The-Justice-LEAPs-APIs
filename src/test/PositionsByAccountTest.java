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
 * GET /positions/account/{accountId}, verified end to end.
 *
 * Fixtures are inserted with raw SQL through JdbcTemplate, never through the code under test,
 * so every assertion checks a value that was written independently of the query that reads it.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
public class PositionsByAccountTest {

    private static final int USER_ID = 900501;
    private static final int ACCOUNT_ID = 900501;
    private static final int OTHER_ACCOUNT_ID = 900502;
    private static final int CLOSED_ACCOUNT_ID = 900503;
    private static final int EMPTY_ACCOUNT_ID = 900504;
    private static final int NEVER_EXISTED_ACCOUNT_ID = 999996;
    private static final int ALPHA_INSTRUMENT_ID = 900501;
    private static final int BETA_INSTRUMENT_ID = 900502;

    private static final int OLDER_OPEN_POSITION_ID = 900601;
    private static final int NEWER_OPEN_POSITION_ID = 900602;
    private static final int CLOSED_POSITION_ID = 900603;
    private static final int OTHER_ACCOUNT_POSITION_ID = 900604;
    private static final int CLOSED_ACCOUNT_POSITION_ID = 900605;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Position Test', 'position.test@example.com', DATE '1990-01-01', '1 Test St', 'SSN_HASH_SENTINEL', 'PASS_HASH_SENTINEL')",
            USER_ID);
        insertAccount(ACCOUNT_ID, true);
        insertAccount(OTHER_ACCOUNT_ID, true);
        insertAccount(CLOSED_ACCOUNT_ID, false);
        insertAccount(EMPTY_ACCOUNT_ID, true);

        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'POS_ALPHA', 'STOCK', 'Alpha Co', 'USD')",
            ALPHA_INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'POS_BETA', 'ETF', 'Beta Fund', 'EUR')",
            BETA_INSTRUMENT_ID);

        insertPosition(OLDER_OPEN_POSITION_ID, ACCOUNT_ID, ALPHA_INSTRUMENT_ID, 10, "12.5000", "125.0000",
            "2026-01-01 10:00:00", null);
        insertPosition(NEWER_OPEN_POSITION_ID, ACCOUNT_ID, BETA_INSTRUMENT_ID, 4, "50.2500", "201.0000",
            "2026-01-03 09:00:00", null);
        // Newest of all, so it would come first if the closed filter were missing
        insertPosition(CLOSED_POSITION_ID, ACCOUNT_ID, ALPHA_INSTRUMENT_ID, 2, "11.0000", "22.0000",
            "2026-01-05 09:00:00", "2026-01-06 09:00:00");
        insertPosition(OTHER_ACCOUNT_POSITION_ID, OTHER_ACCOUNT_ID, ALPHA_INSTRUMENT_ID, 1, "10.0000", "10.0000",
            "2026-01-07 09:00:00", null);
        insertPosition(CLOSED_ACCOUNT_POSITION_ID, CLOSED_ACCOUNT_ID, ALPHA_INSTRUMENT_ID, 1, "10.0000", "10.0000",
            "2026-01-07 09:00:00", null);
    }

    private void insertAccount(int accountId, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 1000.0000, 'BALANCED', 'Passive', now(), ?)",
            accountId, USER_ID, active);
    }

    private void insertPosition(int positionId, int accountId, int instrumentId, int quantity,
                                String averagePrice, String totalPrice, String openedAt, String closedAt) {
        jdbcTemplate.update(
            "INSERT INTO positions (position_id, account_id, instrument_id, quantity, average_price, total_price, opened_at, closed_at) " +
            "VALUES (?, ?, ?, ?, CAST(? AS NUMERIC(18,4)), CAST(? AS NUMERIC(18,4)), CAST(? AS TIMESTAMP), CAST(? AS TIMESTAMP))",
            positionId, accountId, instrumentId, quantity, averagePrice, totalPrice, openedAt, closedAt);
    }

    @Test
    void positionReturnsEveryAcceptanceCriteriaField() throws Exception {
        mockMvc.perform(get("/positions/account/" + ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].position_id").value(OLDER_OPEN_POSITION_ID))
            .andExpect(jsonPath("$[1].account_id").value(ACCOUNT_ID))
            .andExpect(jsonPath("$[1].instrument_id").value(ALPHA_INSTRUMENT_ID))
            .andExpect(jsonPath("$[1].ticker").value("POS_ALPHA"))
            .andExpect(jsonPath("$[1].asset_name").value("Alpha Co"))
            .andExpect(jsonPath("$[1].asset_type").value("STOCK"))
            .andExpect(jsonPath("$[1].currency").value("USD"))
            .andExpect(jsonPath("$[1].quantity").value(10))
            .andExpect(jsonPath("$[1].average_price").value(12.5))
            .andExpect(jsonPath("$[1].total_price").value(125.0))
            .andExpect(jsonPath("$[1].opened_at").value("2026-01-01T10:00:00"));
    }

    @Test
    void instrumentDetailsBelongToEachPositionsOwnInstrument() throws Exception {
        mockMvc.perform(get("/positions/account/" + ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].position_id").value(NEWER_OPEN_POSITION_ID))
            .andExpect(jsonPath("$[0].instrument_id").value(BETA_INSTRUMENT_ID))
            .andExpect(jsonPath("$[0].ticker").value("POS_BETA"))
            .andExpect(jsonPath("$[0].asset_name").value("Beta Fund"))
            .andExpect(jsonPath("$[0].asset_type").value("ETF"))
            .andExpect(jsonPath("$[0].currency").value("EUR"))
            .andExpect(jsonPath("$[0].average_price").value(50.25));
    }

    @Test
    void responseContainsOnlyTheStoryFields() throws Exception {
        mockMvc.perform(get("/positions/account/" + ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].*", hasSize(11)));
    }

    @Test
    void closedPositionsAreExcluded() throws Exception {
        mockMvc.perform(get("/positions/account/" + ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].position_id", not(hasItem(CLOSED_POSITION_ID))));
    }

    @Test
    void returnsOnlyThisAccountsOpenPositionsNewestFirst() throws Exception {
        mockMvc.perform(get("/positions/account/" + ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].position_id", contains(NEWER_OPEN_POSITION_ID, OLDER_OPEN_POSITION_ID)))
            .andExpect(jsonPath("$[*].account_id", everyItem(is(ACCOUNT_ID))));
    }

    @Test
    void accountWithNoPositionsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/positions/account/" + EMPTY_ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(get("/positions/account/" + NEVER_EXISTED_ACCOUNT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void closedAccountIsNotFoundEvenWithPositions() throws Exception {
        mockMvc.perform(get("/positions/account/" + CLOSED_ACCOUNT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}
