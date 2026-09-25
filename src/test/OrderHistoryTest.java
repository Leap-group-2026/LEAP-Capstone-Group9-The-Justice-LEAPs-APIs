package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

import main.Application;
import main.dto.request.CreateOrderRequest;
import main.entities.OrderEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.entities.UserEntity;
import main.entities.PortfolioSize;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.InstrumentRepo;
import main.repos.UserRepo;
import main.repos.HistoricalOrdersRepo;
import main.services.OrderService;
import main.entities.HistoricalOrdersEntity;
import test.config.TestClockConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
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
    
    private OrderEntity testOrder;
    private AccountsEntity testAccount;
    private InstrumentEntity testInstrument;
    private UserEntity testUser;
    
    @BeforeEach
    public void setUp() {
        // Clear old snapshots from previous test runs
        historicalOrdersRepo.deleteAll();
        
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
        accountsRepo.insert(testUser.getUserId(), testAccount.getBalance(), 
                           testAccount.getPortfolioSize().getValue(), testAccount.getTradeType(), null, true);
        
        // Retrieve the created account to get its ID
        java.util.List<AccountsEntity> accounts = accountsRepo.findByUser(testUser.getUserId());
        if (!accounts.isEmpty()) {
            testAccount.setAccountId(accounts.get(0).getAccountId());
        }
        
        // Create a test instrument using MyBatis insert
        testInstrument = new InstrumentEntity();
        testInstrument.setTicker("AAPL");
        testInstrument.setAssetType("stock");
        testInstrument.setAssetName("Apple Inc.");
        testInstrument.setPrice(new BigDecimal("150.00"));
        testInstrument.setCurrency("USD");
        instrumentRepo.insert(testInstrument);
        
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
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))  // At least 1 snapshot on INSERT
            .andExpect(jsonPath("$[0].snapshot", containsString("PENDING")));
    }
    
    @Test
    public void testHistorySnapshotContainsOrderData() throws Exception {
        // Verify the JSONB snapshot has all order information
        // The snapshot now contains only scalar values and IDs (instrument_id, account_id)
        // not entity references like order_id
        mockMvc.perform(get("/orders/" + testOrder.getOrderId() + "/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].snapshot", containsString("side")))
            .andExpect(jsonPath("$[0].snapshot", containsString("quantity")))
            .andExpect(jsonPath("$[0].snapshot", containsString("total_price")))
            .andExpect(jsonPath("$[0].snapshot", containsString("instrument_id")))
            .andExpect(jsonPath("$[0].snapshot", containsString("account_id")))
            .andExpect(jsonPath("$[0].snapshot", containsString("BUY")))
            .andExpect(jsonPath("$[0].snapshot", containsString("status")))
            .andExpect(jsonPath("$[0].snapshot", containsString("PENDING")));
    }
    
    @Test
    public void testHistorySnapshotHasTimestamp() throws Exception {
        // Verify the snapshot has an occurred_at timestamp
        mockMvc.perform(get("/orders/" + testOrder.getOrderId() + "/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].occurred_at", notNullValue()));
    }
    
    @Test
    public void testHistoryEndpointReturnsJsonArray() throws Exception {
        // Verify the endpoint returns valid JSON array
        mockMvc.perform(get("/orders/" + testOrder.getOrderId() + "/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", instanceOf(java.util.ArrayList.class)));
    }
    
    @Test
    public void testHistoryForNonexistentOrder() throws Exception {
        // Try to retrieve history for an order that doesn't exist
        mockMvc.perform(get("/orders/99999/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));  // Should return empty list
    }
}

