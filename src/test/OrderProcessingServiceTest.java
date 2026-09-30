package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import com.fasterxml.jackson.databind.ObjectMapper;

import main.Application;
import main.repos.AccountsRepo;
import main.repos.CurrentPriceRepo;
import main.repos.OrdersRepo;
import main.repos.PositionsRepo;
import main.repos.TransactionsRepo;
import main.services.HistoricalOrdersService;
import main.services.OrderProcessingService;
import main.services.ScheduledOrdersCheck;
import test.config.TestClockConfig;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OrderProcessingService.process, the fill path, verified against the database.
 *
 * Fixtures are inserted with raw SQL through JdbcTemplate and every outcome is read back the same way,
 * so a test can't pass merely because the service's writer and reader agree with each other.
 * TestClockConfig fixes the clock at 12:00 ET on a Tuesday, so the market is open.
 *
 * OrderProcessingService is a spy only so the scheduler tests can make one chosen order throw: no real
 * data makes process() throw any more. Unstubbed calls go to the real method, so every other test is unaffected.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@Transactional
public class OrderProcessingServiceTest {

    private static final int USER_ID = 900701;
    private static final int ACCOUNT_ID = 900701;
    private static final int CLOSED_ACCOUNT_ID = 900702;
    private static final int INSTRUMENT_ID = 900701;
    private static final int UNPRICED_INSTRUMENT_ID = 900702;
    private static final int ORDER_ID = 900801;
    private static final int SECOND_ORDER_ID = 900802;
    private static final int THIRD_ORDER_ID = 900803;
    private static final int EXISTING_POSITION_ID = 900901;

    private static final LocalDateTime FILL_TIME = LocalDateTime.of(2024, 9, 24, 12, 0);

    @SpyBean
    private OrderProcessingService orderProcessingService;

    @Autowired
    private ScheduledOrdersCheck scheduledOrdersCheck;

    @Autowired private OrdersRepo ordersRepo;
    @Autowired private AccountsRepo accountsRepo;
    @Autowired private TransactionsRepo transactionsRepo;
    @Autowired private PositionsRepo positionsRepo;
    @Autowired private CurrentPriceRepo currentPriceRepo;
    @Autowired private HistoricalOrdersService historicalOrdersService;
    @Autowired private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Fill Test', 'fill.test@example.com', DATE '1990-01-01', '1 Test St', 'ssn', 'pass')",
            USER_ID);
        insertAccount(ACCOUNT_ID, "1000.0000", true);
        insertAccount(CLOSED_ACCOUNT_ID, "1000.0000", false);

        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'FILL_PRICED', 'STOCK', 'Priced Co', 'USD')",
            INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, 10.0000, now(), now())",
            INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'FILL_UNPRICED', 'STOCK', 'Unpriced Co', 'USD')",
            UNPRICED_INSTRUMENT_ID);
    }

    private void insertAccount(int accountId, String balance, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, CAST(? AS NUMERIC(18,4)), 'BALANCED', 'Passive', now(), ?)",
            accountId, USER_ID, balance, active);
    }

    private void insertPendingOrder(int accountId, int instrumentId, String side, int quantity) {
        insertPendingOrder(ORDER_ID, accountId, instrumentId, side, quantity);
    }

    private void insertPendingOrder(int orderId, int accountId, int instrumentId, String side, int quantity) {
        jdbcTemplate.update(
            "INSERT INTO orders (order_id, side, account_id, instrument_id, status, quantity, total_price, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, 'PENDING', ?, 0.0000, TIMESTAMP '2024-09-24 11:00:00', TIMESTAMP '2024-09-24 11:00:00')",
            orderId, side, accountId, instrumentId, quantity);
    }

    private void insertOpenPosition(int quantity, String totalPrice, String averagePrice) {
        jdbcTemplate.update(
            "INSERT INTO positions (position_id, account_id, instrument_id, quantity, total_price, average_price, opened_at, closed_at) " +
            "VALUES (?, ?, ?, ?, CAST(? AS NUMERIC(18,4)), CAST(? AS NUMERIC(18,4)), TIMESTAMP '2024-09-01 10:00:00', NULL)",
            EXISTING_POSITION_ID, ACCOUNT_ID, INSTRUMENT_ID, quantity, totalPrice, averagePrice);
    }

    private String orderStatus() {
        return orderStatus(ORDER_ID);
    }

    private String orderStatus(int orderId) {
        return jdbcTemplate.queryForObject("SELECT status FROM orders WHERE order_id = ?", String.class, orderId);
    }

    private List<String> snapshotStatuses(int orderId) {
        return jdbcTemplate.queryForList(
            "SELECT order_information_json FROM historical_orders WHERE order_id = ? ORDER BY historical_order_id",
            String.class, orderId).stream()
            .map(json -> {
                try {
                    return objectMapper.readTree(json).path("status").asText();
                } catch (Exception e) {
                    throw new IllegalStateException("unreadable snapshot: " + json, e);
                }
            })
            .toList();
    }

    private BigDecimal balance(int accountId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, accountId);
    }

    private List<Map<String, Object>> positions() {
        return jdbcTemplate.queryForList(
            "SELECT position_id, quantity, total_price, average_price, opened_at, closed_at FROM positions " +
            "WHERE account_id = ? AND instrument_id = ? ORDER BY position_id", ACCOUNT_ID, INSTRUMENT_ID);
    }

    private List<Map<String, Object>> transactions(int accountId) {
        return jdbcTemplate.queryForList(
            "SELECT amount, side, transaction_type, happened_at FROM transactions WHERE account_id = ?", accountId);
    }

    private static void assertMoney(String expected, Object actual) {
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) actual), "expected " + expected + " but was " + actual);
    }

    // ---- Bug 1: the position branch was inverted ----

    @Test
    void firstBuyOfAnInstrumentOpensAPositionAndFills() {
        insertPendingOrder(ACCOUNT_ID, INSTRUMENT_ID, "BUY", 3);

        orderProcessingService.process(ORDER_ID);

        assertEquals("FILLED", orderStatus());
        List<Map<String, Object>> positions = positions();
        assertEquals(1, positions.size());
        assertEquals(3, positions.get(0).get("quantity"));
        assertMoney("30", positions.get(0).get("total_price"));
        assertMoney("10", positions.get(0).get("average_price"));
        assertEquals(Timestamp.valueOf(FILL_TIME), positions.get(0).get("opened_at"));
        assertNull(positions.get(0).get("closed_at"));
        assertMoney("970", balance(ACCOUNT_ID));
        List<Map<String, Object>> txns = transactions(ACCOUNT_ID);
        assertEquals(1, txns.size());
        assertMoney("30", txns.get(0).get("amount"));
        assertEquals("OUT", txns.get(0).get("side"));
        assertEquals("TRADE", txns.get(0).get("transaction_type"));
    }

    @Test
    void buyIntoAnExistingPositionUpdatesItInsteadOfOpeningAnother() {
        insertOpenPosition(2, "16.0000", "8.0000");
        insertPendingOrder(ACCOUNT_ID, INSTRUMENT_ID, "BUY", 3);

        orderProcessingService.process(ORDER_ID);

        assertEquals("FILLED", orderStatus());
        List<Map<String, Object>> positions = positions();
        assertEquals(1, positions.size(), "a second buy must not open a duplicate position");
        assertEquals(EXISTING_POSITION_ID, positions.get(0).get("position_id"));
        assertEquals(5, positions.get(0).get("quantity"));
        assertMoney("46", positions.get(0).get("total_price"));
        assertMoney("9.2", positions.get(0).get("average_price"));
        assertEquals(Timestamp.valueOf("2024-09-01 10:00:00"), positions.get(0).get("opened_at"));
        assertMoney("970", balance(ACCOUNT_ID));
    }

    // ---- Bug 2: decline carried on instead of returning ----

    @Test
    void orderForUnpricedInstrumentIsDeclinedWithoutSideEffects() {
        insertPendingOrder(ACCOUNT_ID, UNPRICED_INSTRUMENT_ID, "BUY", 3);

        assertDoesNotThrow(() -> orderProcessingService.process(ORDER_ID));

        assertEquals("DECLINED", orderStatus());
        assertMoney("1000", balance(ACCOUNT_ID));
        assertTrue(transactions(ACCOUNT_ID).isEmpty());
    }

    @Test
    void orderOnClosedAccountIsDeclinedWithoutSideEffects() {
        insertPendingOrder(CLOSED_ACCOUNT_ID, INSTRUMENT_ID, "BUY", 3);

        assertDoesNotThrow(() -> orderProcessingService.process(ORDER_ID));

        assertEquals("DECLINED", orderStatus());
        assertMoney("1000", balance(CLOSED_ACCOUNT_ID));
        assertTrue(transactions(CLOSED_ACCOUNT_ID).isEmpty());
    }

    @Test
    void buyBeyondBalanceIsDeclinedWithoutSideEffects() {
        insertPendingOrder(ACCOUNT_ID, INSTRUMENT_ID, "BUY", 101);

        orderProcessingService.process(ORDER_ID);

        assertEquals("DECLINED", orderStatus());
        assertMoney("1000", balance(ACCOUNT_ID));
        assertTrue(positions().isEmpty());
        assertTrue(transactions(ACCOUNT_ID).isEmpty());
    }

    // ---- The existing idempotency guard, which the Kafka consumer will rely on ----

    @Test
    void processingTheSameOrderTwiceFillsItOnce() {
        insertPendingOrder(ACCOUNT_ID, INSTRUMENT_ID, "BUY", 3);

        orderProcessingService.process(ORDER_ID);
        orderProcessingService.process(ORDER_ID);

        assertEquals("FILLED", orderStatus());
        assertMoney("970", balance(ACCOUNT_ID));
        assertEquals(1, transactions(ACCOUNT_ID).size());
        assertEquals(3, positions().get(0).get("quantity"));
    }

    // ---- The scheduler (sweeper) ----

    @Test
    void oneFailingOrderDoesNotStopTheRestOfTheRun() {
        insertPendingOrder(ORDER_ID, ACCOUNT_ID, INSTRUMENT_ID, "BUY", 1);
        insertPendingOrder(SECOND_ORDER_ID, ACCOUNT_ID, INSTRUMENT_ID, "BUY", 1);
        insertPendingOrder(THIRD_ORDER_ID, ACCOUNT_ID, INSTRUMENT_ID, "BUY", 1);
        doThrow(new IllegalStateException("simulated processing failure"))
            .when(orderProcessingService).process(SECOND_ORDER_ID);

        assertDoesNotThrow(() -> scheduledOrdersCheck.processOrders());

        assertEquals("FILLED", orderStatus(ORDER_ID));
        assertEquals("FAILED", orderStatus(SECOND_ORDER_ID));
        assertEquals("FILLED", orderStatus(THIRD_ORDER_ID), "an earlier failure stopped this order being processed");
        List<String> failedSnapshots = snapshotStatuses(SECOND_ORDER_ID);
        assertEquals("FAILED", failedSnapshots.get(failedSnapshots.size() - 1), "FAILED needs its snapshot");
        assertMoney("980", balance(ACCOUNT_ID));
    }

    @Test
    void anOrderFilledMeanwhileIsNotOverwrittenWithFailed() {
        insertPendingOrder(ACCOUNT_ID, INSTRUMENT_ID, "BUY", 3);
        // Simulates a Kafka delivery filling the order while the sweeper's attempt fails
        doAnswer(invocation -> {
            jdbcTemplate.update("UPDATE orders SET status = 'FILLED' WHERE order_id = ?", ORDER_ID);
            throw new IllegalStateException("simulated processing failure");
        }).when(orderProcessingService).process(ORDER_ID);

        try {
            scheduledOrdersCheck.processOrders();
        } catch (RuntimeException rethrown) {
            // Whether the run rethrows is covered by the test above; this one is only about the status
        }

        assertEquals("FILLED", orderStatus());
        assertFalse(snapshotStatuses(ORDER_ID).contains("FAILED"), "a FAILED snapshot was written for a filled order");
    }

    @Test
    void theMarketIsClosedOnSaturdays() {
        insertPendingOrder(ACCOUNT_ID, INSTRUMENT_ID, "BUY", 3);
        // 2024-09-28 is a Saturday; 16:00 UTC is 12:00 ET, inside weekday trading hours
        Clock saturdayNoon = Clock.fixed(Instant.parse("2024-09-28T16:00:00Z"), ZoneId.of("America/New_York"));
        OrderProcessingService onSaturday = new OrderProcessingService(ordersRepo, accountsRepo, transactionsRepo,
            positionsRepo, currentPriceRepo, historicalOrdersService, saturdayNoon);

        assertFalse(onSaturday.isMarketOpen());
        onSaturday.process(ORDER_ID);

        assertEquals("PENDING", orderStatus());
        assertMoney("1000", balance(ACCOUNT_ID));
        assertTrue(transactions(ACCOUNT_ID).isEmpty());
    }
}
