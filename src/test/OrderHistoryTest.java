package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootTest(classes = Application.class)
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
        // Clear old snapshots from previous test runs (old format may have nested objects)
        historicalOrdersRepo.deleteAll();
        
        // Create a test user
        testUser = new UserEntity();
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testUser.setAddress("123 Main St");
        testUser.setSsnHash("hashed_ssn_123");
        testUser.setPassHash("hashed_password_123");
        userRepo.save(testUser);
        
        // Create a test account
        testAccount = new AccountsEntity();
        testAccount.setUserId(testUser);
        testAccount.setBalance(new BigDecimal("10000.00"));
        testAccount.setPortfolioSize(PortfolioSize.BALANCED);
        testAccount.setTradeType("stocks");
        accountsRepo.save(testAccount);
        
        // Create a test instrument
        testInstrument = new InstrumentEntity();
        testInstrument.setTicker("AAPL");
        testInstrument.setAssetType("stock");
        testInstrument.setAssetName("Apple Inc.");
        testInstrument.setPrice(new BigDecimal("150.00"));
        testInstrument.setCurrency("USD");
        instrumentRepo.save(testInstrument);
        
        // Create a test order (and capture initial snapshot)
        testOrder = new OrderEntity("BUY", testAccount, testInstrument, 100, new BigDecimal("15000.00"));
        testOrder.setStatus("PENDING");
        testOrder = orderService.createOrderWithSnapshot(testOrder);  // ← Now captures initial snapshot
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
    
    @Test
    public void testOrderStatusUpdateInDatabase() throws Exception {
        // Verify that updateOrderStatus works and updates the main table
        orderService.updateOrderStatus(testOrder.getOrderId(), "FILLED");
        
        OrderEntity updated = ordersRepo.findById(testOrder.getOrderId()).orElseThrow();
        assert updated.getStatus().equals("FILLED");
        
        // Also verify history was captured (at least the initial one)
        List<HistoricalOrdersEntity> history = historicalOrdersRepo.findByOrderId_OrderIdOrderByCreatedAtAsc(testOrder.getOrderId());
        assert history.size() >= 1;
        assert history.get(0).getOrderInformationJson().contains("PENDING");
    }

    @Test
    public void testMultipleStatusChangesShowCorrectSnapshotState() throws Exception {
        /**
         * CRITICAL TEST: Verify that the snapshot aliasing bug is fixed.
         * 
         * BUG BEHAVIOR (before fix): All events show FINAL status because order_id
         * resolves to the live OrderEntity, which has been mutated.
         * 
         * CORRECT BEHAVIOR (after fix): Each event shows the status *as it was* at
         * that point in time, because the snapshot captures scalar values, not entity refs.
         */
        
        Integer orderId = testOrder.getOrderId();
        
        // Move order through two status transitions
        orderService.updateOrderStatus(orderId, "ACCEPTED");
        orderService.updateOrderStatus(orderId, "FILLED");
        
        // Now fetch the history
        // Timeline:
        // Event 0: Initial creation (PENDING)
        // Event 1: Before 1st update (PENDING)
        // Event 2: After 1st update (ACCEPTED)
        // Event 3: Before 2nd update (ACCEPTED)
        // Event 4: After 2nd update (FILLED)
        mockMvc.perform(get("/orders/" + orderId + "/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))  // 1 initial + 2 per update = 5 events
            // Event 0: Initial order creation (PENDING)
            .andExpect(jsonPath("$[0].status", equalTo("PENDING")))
            .andExpect(jsonPath("$[0].snapshot", containsString("PENDING")))
            // Event 1: Before first update (still PENDING)
            .andExpect(jsonPath("$[1].status", equalTo("PENDING")))
            .andExpect(jsonPath("$[1].snapshot", containsString("PENDING")))
            // Event 2: After first update (ACCEPTED)
            .andExpect(jsonPath("$[2].status", equalTo("ACCEPTED")))
            .andExpect(jsonPath("$[2].snapshot", containsString("ACCEPTED")))
            // Event 3: Before second update (still ACCEPTED)
            .andExpect(jsonPath("$[3].status", equalTo("ACCEPTED")))
            .andExpect(jsonPath("$[3].snapshot", containsString("ACCEPTED")))
            // Event 4: After second update (FILLED)
            .andExpect(jsonPath("$[4].status", equalTo("FILLED")))
            .andExpect(jsonPath("$[4].snapshot", containsString("FILLED")));
    }
}

