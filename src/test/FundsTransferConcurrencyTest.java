import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

import main.Application;
import exception.InsufficientFundsException;
import services.TransactionsService;
import test.config.TestClockConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Concurrent deposits and withdrawals on one account must neither overdraw it nor lose an update,
 * and the balance must always equal the opening balance plus the transaction rows.
 *
 * Deliberately NOT @Transactional: each service call needs its own real transaction for the
 * row lock to matter, so fixtures are committed and removed again in tearDown.
 * The service is called directly, so the threads don't share a MockMvc or security context.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
public class FundsTransferConcurrencyTest {

    private static final int USER_ID = 940201;
    private static final int ACCOUNT_ID = 940201;
    private static final int ROUNDS = 10;
    private static final int DEPOSITS = 20;

    @Autowired
    private TransactionsService transactionsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        tearDown();
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Funds Race Test', 'funds.race.test@example.com', DATE '1990-01-01', '1 Test St', 'ssn', 'pass')",
            USER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 100.0000, 'BALANCED', 'Passive', now(), TRUE)",
            ACCOUNT_ID, USER_ID);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM transactions WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM accounts WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM user_info WHERE user_id = ?", USER_ID);
    }

    private BigDecimal balance() {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, ACCOUNT_ID);
    }

    private BigDecimal ledger() {
        return jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(CASE side WHEN 'IN' THEN amount ELSE -amount END), 0) FROM transactions WHERE account_id = ?",
            BigDecimal.class, ACCOUNT_ID);
    }

    // Runs every task at once and returns how many succeeded; rethrows anything but InsufficientFundsException
    private int runTogether(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CyclicBarrier start = new CyclicBarrier(tasks.size());
        List<Future<Object>> results = new ArrayList<>();
        for (Callable<Object> task : tasks) {
            results.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        int succeeded = 0;
        try {
            for (Future<Object> result : results) {
                try {
                    result.get(60, TimeUnit.SECONDS);
                    succeeded++;
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof InsufficientFundsException)) {
                        throw e;
                    }
                }
            }
        } finally {
            pool.shutdown();
        }
        return succeeded;
    }

    @Test
    void twoWithdrawalsThatTogetherExceedTheBalanceLetExactlyOneThrough() throws Exception {
        // Repeated because a single pair may happen not to overlap; every round must hold
        for (int round = 0; round < ROUNDS; round++) {
            jdbcTemplate.update("DELETE FROM transactions WHERE account_id = ?", ACCOUNT_ID);
            jdbcTemplate.update("UPDATE accounts SET balance = 100.0000 WHERE account_id = ?", ACCOUNT_ID);

            List<Callable<Object>> withdrawals = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                withdrawals.add(() -> transactionsService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("60.00")));
            }
            int succeeded = runTogether(withdrawals);

            assertEquals(1, succeeded, "round " + round + ": two $60 withdrawals from $100 must not both succeed");
            assertEquals(0, new BigDecimal("40").compareTo(balance()), "round " + round + ": balance was " + balance());
            assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM transactions WHERE account_id = ? AND side = 'OUT'", Integer.class, ACCOUNT_ID));
            assertEquals(0, new BigDecimal("100").add(ledger()).compareTo(balance()),
                "round " + round + ": balance " + balance() + " does not equal 100 + ledger " + ledger());
        }
    }

    @Test
    void concurrentDepositsAreAllCredited() throws Exception {
        List<Callable<Object>> deposits = new ArrayList<>();
        for (int i = 0; i < DEPOSITS; i++) {
            deposits.add(() -> transactionsService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("10.00")));
        }
        assertEquals(DEPOSITS, runTogether(deposits));

        assertEquals(0, new BigDecimal("300").compareTo(balance()),
            "20 deposits of $10 onto $100 must leave $300, a lower balance means a deposit was lost; was " + balance());
        assertEquals(DEPOSITS, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM transactions WHERE account_id = ? AND side = 'IN'", Integer.class, ACCOUNT_ID));
        assertEquals(0, new BigDecimal("100").add(ledger()).compareTo(balance()));
    }

    @Test
    void mixedDepositsAndWithdrawalsKeepTheBalanceAndLedgerInAgreement() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> transactionsService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("15.00")));
            tasks.add(() -> transactionsService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("25.00")));
        }
        runTogether(tasks);

        BigDecimal balance = balance();
        assertTrue(balance.signum() >= 0, "the account was overdrawn: " + balance);
        assertEquals(0, new BigDecimal("100").add(ledger()).compareTo(balance),
            "balance " + balance + " does not equal 100 + ledger " + ledger());
    }
}
