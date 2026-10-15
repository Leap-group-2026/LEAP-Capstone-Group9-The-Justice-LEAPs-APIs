import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

import main.Application;
import dto.request.TransactionRequest;
import services.UserService;
import test.config.TestClockConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Concurrent USD exchanges on one account must not lose balance updates.
 *
 * Deliberately NOT @Transactional: each currencyExchange() call needs its own real transaction for the
 * row locks to matter, so fixtures are committed and removed again in tearDown.
 * Every exchange is into a different currency with an existing position, so the only row the exchanges
 * share is the account; that isolates the balance read from anything else that might serialise them.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
public class CurrencyExchangeConcurrencyTest {

    private static final int USER_ID = 901101;
    private static final int ACCOUNT_ID = 901101;
    private static final int FIRST_ID = 901101;
    private static final int EXCHANGES = 20;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        tearDown();
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Fx Race Test', 'fx.race.test@example.com', DATE '1990-01-01', '1 Test St', 'ssn', 'pass')",
            USER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 10000.0000, 'BALANCED', 'Passive', now(), TRUE)",
            ACCOUNT_ID, USER_ID);
        for (int i = 0; i < EXCHANGES; i++) {
            int id = FIRST_ID + i;
            jdbcTemplate.update(
                "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, ?, 'FOREX', 'Race Fx', ?)",
                id, "USD/RX" + i, "RX" + i);
            jdbcTemplate.update(
                "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, 1.0000, now(), now())", id);
            jdbcTemplate.update(
                "INSERT INTO positions (position_id, account_id, instrument_id, quantity, total_price, average_price, opened_at) " +
                "VALUES (?, ?, ?, 1, 10.0000, 1.0000, TIMESTAMP '2024-09-01 10:00:00')",
                id, ACCOUNT_ID, id);
        }
    }

    @AfterEach
    void tearDown() {
        int last = FIRST_ID + EXCHANGES - 1;
        jdbcTemplate.update("DELETE FROM transactions WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM positions WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM current_prices WHERE instrument_id BETWEEN ? AND ?", FIRST_ID, last);
        jdbcTemplate.update("DELETE FROM instruments WHERE instrument_id BETWEEN ? AND ?", FIRST_ID, last);
        jdbcTemplate.update("DELETE FROM accounts WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM user_info WHERE user_id = ?", USER_ID);
    }

    @Test
    void concurrentExchangesOnOneAccountDebitEveryExchange() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(EXCHANGES);
        CyclicBarrier start = new CyclicBarrier(EXCHANGES);
        List<Future<?>> results = new ArrayList<>();
        for (int i = 0; i < EXCHANGES; i++) {
            TransactionRequest request = new TransactionRequest(
                "USD/RX" + i, new BigDecimal("10.00"), "BUY", ACCOUNT_ID, "EXCHANGE");
            results.add(pool.submit(() -> {
                start.await();
                assertEquals(200, userService.currencyExchange(request).getStatusCode().value());
                return null;
            }));
        }
        for (Future<?> result : results) {
            result.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        BigDecimal balance = jdbcTemplate.queryForObject(
            "SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, ACCOUNT_ID);
        assertEquals(0, new BigDecimal("9800").compareTo(balance),
            "20 exchanges of $10 from $10000 must leave $9800, a higher balance means an exchange's debit was lost; was " + balance);
    }
}
