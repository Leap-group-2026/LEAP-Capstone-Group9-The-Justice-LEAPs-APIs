import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import main.Application;
import entities.TransactionsEntity;
import repos.TransactionsRepo;
import services.TransactionsService;
import test.config.TestClockConfig;

import java.math.BigDecimal;

/**
 * The balance change and its transaction row must be saved together or not at all: a balance that
 * moved without a row would make the history disagree with the account.
 *
 * Deliberately NOT @Transactional: a test-wide transaction would hide whether the service
 * rolls back on its own. Fixtures are raw SQL and are removed in tearDown.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
public class FundsTransferAtomicityTest {

    private static final int USER_ID = 940301;
    private static final int ACCOUNT_ID = 940301;

    @Autowired
    private TransactionsService transactionsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Stands in for the database rejecting the transactions insert, after the balance has been updated
    @MockBean
    private TransactionsRepo transactionsRepo;

    @BeforeEach
    void setUp() {
        tearDown();
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Funds Atomicity Test', 'funds.atomicity.test@example.com', DATE '1990-01-01', '1 Test St', 'ssn', 'pass')",
            USER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 1000.0000, 'BALANCED', 'Passive', now(), true)",
            ACCOUNT_ID, USER_ID);
        doThrow(new DataIntegrityViolationException("transactions insert rejected"))
            .when(transactionsRepo).insertReturningId(any(TransactionsEntity.class));
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

    @Test
    void depositIsUndoneWhenItsTransactionRowFails() {
        assertThrows(DataIntegrityViolationException.class,
            () -> transactionsService.deposit(ACCOUNT_ID, USER_ID, new BigDecimal("250.00")));

        assertEquals(0, new BigDecimal("1000").compareTo(balance()), "deposit was credited without its transaction row; balance " + balance());
    }

    @Test
    void withdrawalIsUndoneWhenItsTransactionRowFails() {
        assertThrows(DataIntegrityViolationException.class,
            () -> transactionsService.withdraw(ACCOUNT_ID, USER_ID, new BigDecimal("250.00")));

        assertEquals(0, new BigDecimal("1000").compareTo(balance()), "withdrawal was debited without its transaction row; balance " + balance());
    }
}
