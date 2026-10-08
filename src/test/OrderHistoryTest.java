import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.hamcrest.Matchers.*;

import main.Application;
import dto.request.CreateOrderRequest;
import entities.OrderEntity;
import entities.AccountsEntity;
import entities.InstrumentEntity;
import entities.UserEntity;
import entities.PortfolioSize;
import repos.OrdersRepo;
import repos.AccountsRepo;
import repos.InstrumentRepo;
import repos.UserRepo;
import repos.HistoricalOrdersRepo;
import services.OrderService;
import entities.HistoricalOrdersEntity;
import test.config.TestClockConfig;
import test.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest(classes = Application.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
@Transactional
public class OrderHistoryTest {
    
    @Autowired
    MockMvc mockMvc;
    
    @Autowired
    OrdersRepo ordersRepo;
    
    @Autowired
    AccountsRepo accountsRepo;
    
    @Autowired
    InstrumentRepo instrumentRepo;
    
    @Autowired
    UserRepo userRepo;
    
    @Autowired
    OrderService orderService;
    
    @Autowired
    HistoricalOrdersRepo historicalOrdersRepo;
    
    @Autowired
    JdbcTemplate jdbcTemplate;
    
    @Autowired
    ObjectMapper objectMapper;
    
    private OrderEntity testOrder;
    private AccountsEntity testAccount;
    private InstrumentEntity testInstrument;
    private UserEntity testUser;
    
    @BeforeEach
    public void setUp() {
        // Create a test user using MyBatis insert
        testUser = new UserEntity();
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testUser.setAddress("123 Main St");
        testUser.setSsnHash("hashed_ssn_123");
        testUser.setPassHash("hashed_password_123");
        userRepo.insert(testUser);
        
        // Create a test account using MyBatis insert
        testAccount = new AccountsEntity();
        testAccount.setUserId(testUser);
        testAccount.setBalance(new BigDecimal("20000.00"));
        testAccount.setPortfolioSize(PortfolioSize.BALANCED);
        testAccount.setTradeType("stocks");
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setAccountActive(true);
        accountsRepo.insert(testAccount);
        // testAccount.accountId is now set by MyBatis via @Options
        
        // Create a test instrument using MyBatis insert
        testInstrument = new InstrumentEntity();
        testInstrument.setTicker("AAPL");
        testInstrument.setAssetType("stock");
        testInstrument.setAssetName("Apple Inc.");
        testInstrument.setCurrency("USD");
        instrumentRepo.insert(testInstrument);
        
        // Seed current_prices directly; CurrentPriceRepo.upsert uses Postgres-only ON CONFLICT
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, ?, now(), now())",
            testInstrument.getInstrumentId(), new BigDecimal("150.00"));
        
        // Create a test order using the simplified createOrder method
        CreateOrderRequest request = new CreateOrderRequest("BUY", testAccount.getAccountId(), testInstrument.getInstrumentId(), 100);
        // Note: Simplified OrderService.createOrder() returns OrderSubmissionResponse, not OrderEntity
        // For now, we'll just retrieve the order from the database after creation
        orderService.createOrder(request);
        
        // Get the created order from database (using the most recent order)
        List<OrderEntity> orders = ordersRepo.findAll();
        if (!orders.isEmpty()) {
            testOrder = orders.get(orders.size() - 1);
        }
    }
    
    @Test
    public void testInitialOrderCreatesSnapshot() throws Exception {
        // When an order is created, the trigger should capture a snapshot
        mockMvc.perform(get("/orders/" + testOrder.getOrderId() + "/history")
            .with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))  // At least 1 snapshot on INSERT
            .andExpect(jsonPath("$[0].snapshot", containsString("PENDING")));
    }
    
    @Test
    public void testHistorySnapshotContainsOrderData() throws Exception {
        // Verify the JSONB snapshot has all order information
        // The snapshot now contains only scalar values and IDs (camelCase: instrumentId, accountId)
        // not entity references like order_id
        mockMvc.perform(get("/orders/" + testOrder.getOrderId() + "/history")
            .with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            // Raw JSON snapshot should contain camelCase field names (new format)
            .andExpect(jsonPath("$[0].snapshot", containsString("side")))
            .andExpect(jsonPath("$[0].snapshot", containsString("quantity")))
            .andExpect(jsonPath("$[0].snapshot", containsString("totalPrice")))
            .andExpect(jsonPath("$[0].snapshot", containsString("instrumentId")))
            .andExpect(jsonPath("$[0].snapshot", containsString("accountId")))
            .andExpect(jsonPath("$[0].snapshot", containsString("BUY")))
            .andExpect(jsonPath("$[0].snapshot", containsString("status")))
            .andExpect(jsonPath("$[0].snapshot", containsString("PENDING")))
            // Deserialized response fields should also be populated (use snake_case for JSON)
            .andExpect(jsonPath("$[0].instrument_id").value(testInstrument.getInstrumentId()))
            .andExpect(jsonPath("$[0].total_price").value(15000.0))
            .andExpect(jsonPath("$[0].account_id").value(testAccount.getAccountId()))
            .andExpect(jsonPath("$[0].snapshot_updated_at", notNullValue()));
    }
    
    @Test
    public void testHistorySnapshotHasTimestamp() throws Exception {
        // Verify the snapshot has an occurred_at timestamp
        mockMvc.perform(get("/orders/" + testOrder.getOrderId() + "/history")
            .with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].occurred_at", notNullValue()));
    }
    
    @Test
    public void testHistoryEndpointReturnsJsonArray() throws Exception {
        // Verify the endpoint returns valid JSON array
        mockMvc.perform(get("/orders/" + testOrder.getOrderId() + "/history")
            .with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", instanceOf(java.util.ArrayList.class)));
    }
    
    @Test
    public void testHistoryForNonexistentOrder() throws Exception {
        // Try to retrieve history for an order that doesn't exist
        mockMvc.perform(get("/orders/99999/history")
            .with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));  // Should return empty list
    }
    
    @Test
    public void testHistoryDeserializesCamelCaseSnapshot() throws Exception {
        // Directly insert a snapshot with camelCase field names (the current canonical format)
        String camelCaseSnapshotJson = "{" +
            "\"status\":\"FILLED\"," +
            "\"side\":\"BUY\"," +
            "\"quantity\":50," +
            "\"totalPrice\":7500.00," +
            "\"instrumentId\":" + testInstrument.getInstrumentId() + "," +
            "\"accountId\":" + testAccount.getAccountId() + "," +
            "\"updatedAt\":\"2026-09-28T12:00:00\"" +
            "}";
        
        // Create an order and manually insert a camelCase snapshot
        String createOrderJson = "{\"side\": \"BUY\", \"accountId\": " + testAccount.getAccountId()
            + ", \"instrumentId\": " + testInstrument.getInstrumentId() + ", \"quantity\": 50}";
        
        MvcResult createResult = mockMvc.perform(post("/orders").with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON)
            .content(createOrderJson))
            .andExpect(status().isOk())
            .andReturn();
        
        Integer orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("order_id").asInt();
        
        // Directly insert the camelCase snapshot into the database
        jdbcTemplate.update(
            "INSERT INTO historical_orders (order_id, account_id, order_information_json, created_at) VALUES (?, ?, ?, now())",
            orderId, testAccount.getAccountId(), camelCaseSnapshotJson);
        
        // Read it back through the endpoint and verify all fields deserialize
        mockMvc.perform(get("/orders/" + orderId + "/history")
            .with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].status").value("FILLED"))  // Index 1 is the manually inserted one
            .andExpect(jsonPath("$[1].side").value("BUY"))
            .andExpect(jsonPath("$[1].quantity").value(50))
            .andExpect(jsonPath("$[1].total_price").value(7500.0))
            .andExpect(jsonPath("$[1].instrument_id").value(testInstrument.getInstrumentId()))
            .andExpect(jsonPath("$[1].account_id").value(testAccount.getAccountId()))
            .andExpect(jsonPath("$[1].snapshot_updated_at", notNullValue()));
    }
    
    @Test
    public void testHistoryDeserializesSnakeCaseSnapshot() throws Exception {
        // Directly insert a snapshot with snake_case field names (the previous build's format).
        // No production row uses it any more; this proves the @JsonAlias rollback insurance works.
        String oldSnapshotJson = "{" +
            "\"status\":\"CANCELED\"," +
            "\"side\":\"BUY\"," +
            "\"quantity\":25," +
            "\"total_price\":3750.00," +
            "\"instrument_id\":" + testInstrument.getInstrumentId() + "," +
            "\"account_id\":" + testAccount.getAccountId() + "," +
            "\"updated_at\":\"2026-09-28T11:00:00\"" +
            "}";
        
        // Create an order and manually insert a snake_case snapshot
        String createOrderJson = "{\"side\": \"BUY\", \"accountId\": " + testAccount.getAccountId()
            + ", \"instrumentId\": " + testInstrument.getInstrumentId() + ", \"quantity\": 25}";
        
        MvcResult createResult = mockMvc.perform(post("/orders").with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON)
            .content(createOrderJson))
            .andExpect(status().isOk())
            .andReturn();
        
        Integer orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("order_id").asInt();
        
        // Directly insert the snake_case snapshot into the database
        jdbcTemplate.update(
            "INSERT INTO historical_orders (order_id, account_id, order_information_json, created_at) VALUES (?, ?, ?, now())",
            orderId, testAccount.getAccountId(), oldSnapshotJson);
        
        // Read it back through the endpoint and verify all fields deserialize
        mockMvc.perform(get("/orders/" + orderId + "/history")
            .with(user(String.valueOf(testUser.getUserId())))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].status").value("CANCELED"))  // Index 1 is the manually inserted one
            .andExpect(jsonPath("$[1].side").value("BUY"))
            .andExpect(jsonPath("$[1].quantity").value(25))
            .andExpect(jsonPath("$[1].total_price").value(3750.0))
            .andExpect(jsonPath("$[1].instrument_id").value(testInstrument.getInstrumentId()))
            .andExpect(jsonPath("$[1].account_id").value(testAccount.getAccountId()))
            .andExpect(jsonPath("$[1].snapshot_updated_at", notNullValue()));
    }
}

