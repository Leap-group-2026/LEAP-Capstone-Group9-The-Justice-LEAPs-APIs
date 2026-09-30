package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import main.Application;
import main.repos.HistoricalOrdersRepo;
import test.config.TestClockConfig;

/**
 * An order and its audit snapshot must be saved together or not at all. historical_orders is a
 * regulatory audit log, so an order without its snapshot is a compliance gap, not just bad data.
 *
 * Deliberately NOT @Transactional: a test-wide transaction would hide whether the service
 * commits the order on its own. Fixtures are raw SQL and are removed in tearDown.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
public class OrderAtomicityTest {

    private static final int USER_ID = 930001;
    private static final int ACCOUNT_ID = 930001;
    private static final int INSTRUMENT_ID = 930001;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Stands in for the real failure seen on Postgres: the snapshot insert rejected by the database
    @MockBean
    private HistoricalOrdersRepo historicalOrdersRepo;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Atomicity Test', 'atomicity.test@example.com', DATE '1990-01-01', '1 Test St', 'ssn', 'pass')",
            USER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 1000.0000, 'BALANCED', 'Passive', now(), true)",
            ACCOUNT_ID, USER_ID);
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'ATOMIC', 'STOCK', 'Atomic Co', 'USD')",
            INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, 10.0000, now(), now())",
            INSTRUMENT_ID);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM orders WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM current_prices WHERE instrument_id = ?", INSTRUMENT_ID);
        jdbcTemplate.update("DELETE FROM instruments WHERE instrument_id = ?", INSTRUMENT_ID);
        jdbcTemplate.update("DELETE FROM accounts WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM user_info WHERE user_id = ?", USER_ID);
    }

    @Test
    void orderIsNotSavedWhenItsSnapshotFails() {
        doThrow(new DataIntegrityViolationException("snapshot insert rejected"))
            .when(historicalOrdersRepo).insert(anyInt(), anyInt(), anyString(), any());

        String body = "{\"side\":\"BUY\",\"accountId\":" + ACCOUNT_ID
            + ",\"instrumentId\":" + INSTRUMENT_ID + ",\"quantity\":1}";

        // The unhandled failure surfaces as a servlet exception (a 500 in the running app)
        assertThrows(Exception.class, () ->
            mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body)));

        Integer orders = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM orders WHERE account_id = ?", Integer.class, ACCOUNT_ID);
        assertEquals(0, orders, "order was committed without its audit snapshot");
    }
}
