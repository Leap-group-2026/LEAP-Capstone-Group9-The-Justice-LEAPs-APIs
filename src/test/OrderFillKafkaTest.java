import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import main.Application;
import entities.OrderEntity;
import events.OrderSubmittedEvent;
import services.HistoricalOrdersService;
import test.config.TestClockConfig;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.UUID;

/**
 * The Kafka path end to end, against an embedded broker and H2.
 *
 * Deliberately NOT @Transactional: the consumer runs on its own threads in its own transactions and
 * can't see an uncommitted test transaction, and AFTER_COMMIT publishing never fires if the test rolls back.
 * Fixtures are committed with JdbcTemplate and removed again in tearDown.
 */
@SpringBootTest(classes = Application.class, properties = {
    "app.kafka.enabled=true",
    "app.kafka.topic.order-submitted=order.submitted",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    "spring.kafka.producer.properties.spring.json.add.type.headers=false",
    "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
    "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
    "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
    "spring.kafka.consumer.properties.spring.json.value.default.type=events.OrderSubmittedEvent",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=events",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@EmbeddedKafka(partitions = 3, topics = {"order.submitted", "order.submitted.DLT"})
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
public class OrderFillKafkaTest {

    private static final String TOPIC = "order.submitted";
    private static final int USER_ID = 901101;
    private static final int ACCOUNT_ID = 901101;
    private static final int INSTRUMENT_ID = 901101;
    private static final int ORDER_ID = 901101;

    @Autowired
    private KafkaTemplate<String, OrderSubmittedEvent> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Real behaviour unless a test stubs it; only the rollback test does, to make an order's snapshot fail
    @SpyBean
    private HistoricalOrdersService historicalOrdersService;

    @BeforeEach
    void setUp() {
        tearDown();
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Kafka Test', 'kafka.test@example.com', DATE '1990-01-01', '1 Test St', 'ssn', 'pass')",
            USER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 1000.0000, 'BALANCED', 'Passive', now(), TRUE)",
            ACCOUNT_ID, USER_ID);
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'KAFKA_CO', 'STOCK', 'Kafka Co', 'USD')",
            INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, 10.0000, now(), now())",
            INSTRUMENT_ID);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM historical_orders WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM transactions WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM positions WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM orders WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM current_prices WHERE instrument_id = ?", INSTRUMENT_ID);
        jdbcTemplate.update("DELETE FROM instruments WHERE instrument_id = ?", INSTRUMENT_ID);
        jdbcTemplate.update("DELETE FROM accounts WHERE account_id = ?", ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM user_info WHERE user_id = ?", USER_ID);
    }

    private void insertPendingBuy(int quantity) {
        jdbcTemplate.update(
            "INSERT INTO orders (order_id, side, account_id, instrument_id, status, quantity, total_price) " +
            "VALUES (?, 'BUY', ?, ?, 'PENDING', ?, 30.0000)",
            ORDER_ID, ACCOUNT_ID, INSTRUMENT_ID, quantity);
    }

    private OrderSubmittedEvent eventFor(int orderId) {
        return new OrderSubmittedEvent(UUID.randomUUID(), orderId, ACCOUNT_ID, INSTRUMENT_ID, "BUY", 3,
            new BigDecimal("30.0000"), LocalDateTime.of(2024, 9, 24, 11, 0), 1);
    }

    // Committed offset = how many messages the group has finished with. Waiting on this, rather than on the
    // balance, is what proves a duplicate was actually consumed: the balance is already right after the first.
    private long committedOffsets(String groupId) throws Exception {
        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()))) {
            return admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get()
                .values().stream().filter(Objects::nonNull).mapToLong(OffsetAndMetadata::offset).sum();
        }
    }

    private long deadLetterCount() throws Exception {
        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()))) {
            Map<TopicPartition, OffsetSpec> latest = IntStream.range(0, 3).boxed()
                .collect(Collectors.toMap(p -> new TopicPartition(TOPIC + ".DLT", p), p -> OffsetSpec.latest()));
            return admin.listOffsets(latest).all().get().values().stream().mapToLong(o -> o.offset()).sum();
        }
    }

    private void awaitConsumed(String groupId, long target) {
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
            .until(() -> committedOffsets(groupId) >= target);
    }

    private String snapshotStatus(String json) {
        try {
            return objectMapper.readTree(json).path("status").asText();
        } catch (Exception e) {
            throw new IllegalStateException("unreadable snapshot: " + json, e);
        }
    }

    private long publishedCount() throws Exception {
        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()))) {
            Map<TopicPartition, OffsetSpec> latest = IntStream.range(0, 3).boxed()
                .collect(Collectors.toMap(p -> new TopicPartition(TOPIC, p), p -> OffsetSpec.latest()));
            return admin.listOffsets(latest).all().get().values().stream().mapToLong(o -> o.offset()).sum();
        }
    }

    private int placeOrder(String side, int quantity) throws Exception {
        String body = "{\"side\":\"" + side + "\",\"accountId\":" + ACCOUNT_ID
            + ",\"instrumentId\":" + INSTRUMENT_ID + ",\"quantity\":" + quantity + "}";
        String response = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        int orderId = objectMapper.readTree(response).path("order_id").asInt();
        assertTrue(orderId > 0, "no order_id in response: " + response);
        return orderId;
    }

    // The story's promise: filled within 5 seconds of being placed
    private void awaitFilledWithinFiveSeconds(int orderId) {
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).until(() -> "FILLED".equals(
            jdbcTemplate.queryForObject("SELECT status FROM orders WHERE order_id = ?", String.class, orderId)));
    }

    private BigDecimal balance() {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, ACCOUNT_ID);
    }

    @Test
    void sameEventDeliveredTwiceFillsTheOrderOnce() throws Exception {
        insertPendingBuy(3);
        long before = committedOffsets("order-fill");
        long deadLettersBefore = deadLetterCount();
        OrderSubmittedEvent event = eventFor(ORDER_ID);

        kafkaTemplate.send(TOPIC, String.valueOf(ACCOUNT_ID), event).get();
        kafkaTemplate.send(TOPIC, String.valueOf(ACCOUNT_ID), event).get();
        awaitConsumed("order-fill", before + 2);

        assertEquals("FILLED", jdbcTemplate.queryForObject(
            "SELECT status FROM orders WHERE order_id = ?", String.class, ORDER_ID));
        assertEquals(0, new BigDecimal("970").compareTo(balance()), "balance moved more than once: " + balance());
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM historical_orders WHERE order_id = ?", Integer.class, ORDER_ID),
            "one FILLED snapshot: the fill ran once");
        assertEquals(deadLettersBefore, deadLetterCount(), "the duplicate should be absorbed, not dead-lettered");
    }

    @Test
    void buyPlacedThroughTheApiIsPublishedOnceAndFilledWithinFiveSeconds() throws Exception {
        long publishedBefore = publishedCount();

        int orderId = placeOrder("BUY", 3);
        awaitFilledWithinFiveSeconds(orderId);

        assertEquals(publishedBefore + 1, publishedCount(), "each order must publish exactly one message");
        assertEquals(0, new BigDecimal("970").compareTo(balance()));
        assertEquals(List.of("PENDING", "FILLED"), jdbcTemplate.queryForList(
            "SELECT order_information_json FROM historical_orders WHERE order_id = ? ORDER BY historical_order_id",
            String.class, orderId).stream().map(this::snapshotStatus).toList());
    }

    @Test
    void sellPlacedThroughTheApiIsFilledWithinFiveSeconds() throws Exception {
        jdbcTemplate.update(
            "INSERT INTO positions (account_id, instrument_id, quantity, total_price, average_price, opened_at) " +
            "VALUES (?, ?, 5, 40.0000, 8.0000, TIMESTAMP '2024-09-01 10:00:00')",
            ACCOUNT_ID, INSTRUMENT_ID);

        int orderId = placeOrder("SELL", 2);
        awaitFilledWithinFiveSeconds(orderId);

        assertEquals(0, new BigDecimal("1020").compareTo(balance()));
        assertEquals(3, jdbcTemplate.queryForObject(
            "SELECT quantity FROM positions WHERE account_id = ? AND instrument_id = ?", Integer.class, ACCOUNT_ID, INSTRUMENT_ID));
        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM transactions WHERE account_id = ?", Integer.class, ACCOUNT_ID),
            "trades are not recorded in transactions");
    }

    @Test
    void rolledBackOrderPublishesNothing() throws Exception {
        // Let the order and snapshot be written and the event be published, then fail the commit itself.
        // A publisher that sent immediately (e.g. @EventListener) would already have sent by then.
        doAnswer(invocation -> {
            invocation.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("simulated failure at commit");
                }
            });
            return null;
        }).when(historicalOrdersService).captureOrderSnapshot(any(OrderEntity.class));
        long publishedBefore = publishedCount();
        String body = "{\"side\":\"BUY\",\"accountId\":" + ACCOUNT_ID
            + ",\"instrumentId\":" + INSTRUMENT_ID + ",\"quantity\":3}";

        // The unhandled failure surfaces as a servlet exception (a 500 in the running app)
        assertThrows(Exception.class, () ->
            mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body)));

        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM orders WHERE account_id = ?", Integer.class, ACCOUNT_ID), "the order should have rolled back");
        // Nothing may be published at all, so watch for a while rather than checking once
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
            .until(() -> publishedCount() == publishedBefore);
    }

    /**
     * Kafka unreachable: nothing listens on localhost:1. OVERRIDE gives this class its own Spring context
     * without the embedded broker. It shares the in-memory H2 database with the outer context, so the
     * outer setUp/tearDown fixtures apply here too.
     */
    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    @SpringBootTest(classes = Application.class, properties = {
        "app.kafka.enabled=true",
        "app.kafka.topic.order-submitted=order.submitted",
        "spring.kafka.bootstrap-servers=localhost:1",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "spring.kafka.producer.properties.max.block.ms=2000",
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false"
    })
    @Import(TestClockConfig.class)
    @AutoConfigureMockMvc
    class WhenKafkaIsDown {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void orderIsStillSavedAsPendingAndTheRequestDoesNotHang() throws Exception {
            String body = "{\"side\":\"BUY\",\"accountId\":" + ACCOUNT_ID
                + ",\"instrumentId\":" + INSTRUMENT_ID + ",\"quantity\":3}";

            long start = System.nanoTime();
            String response = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            int orderId = objectMapper.readTree(response).path("order_id").asInt();
            assertTrue(orderId > 0, "no order_id in response: " + response);
            // The first snapshot, not the current status: a scheduler in another cached test context may
            // legitimately fill the order moments later, which is the sweeper doing its job
            List<String> snapshots = jdbcTemplate.queryForList(
                "SELECT order_information_json FROM historical_orders WHERE order_id = ? ORDER BY historical_order_id",
                String.class, orderId);
            assertFalse(snapshots.isEmpty(), "order " + orderId + " was not saved with its snapshot");
            assertEquals("PENDING", snapshotStatus(snapshots.get(0)));
            // max.block.ms is 2000 here; the bound is loose so a slow CI machine doesn't make this flaky
            assertTrue(elapsedMillis < 6000, "POST /orders took " + elapsedMillis + " ms with Kafka down");
        }
    }
}
