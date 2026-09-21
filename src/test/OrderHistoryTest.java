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
import main.entities.accountsEntity;
import main.entities.instrumentEntity;
import main.entities.userEntity;
import main.entities.PortfolioSize;
import main.repos.OrdersRepo;
import main.repos.AccountsRepo;
import main.repos.instrumentRepo;
import main.repos.userRepo;
import main.repos.historicalOrdersRepo;
import main.services.OrderService;
import main.entities.historicalOrdersEntity;

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
    instrumentRepo instrumentRepo;
    
    @Autowired
    userRepo userRepo;
    
    @Autowired
    OrderService orderService;
    
    @Autowired
    historicalOrdersRepo historicalOrdersRepo;
    
    private OrderEntity testOrder;
    private accountsEntity testAccount;
    private instrumentEntity testInstrument;
    private userEntity testUser;
    
    @BeforeEach
    public void setUp() {
        // Create a test user
        testUser = new userEntity();
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testUser.setAddress("123 Main St");
        testUser.setSsnHash("hashed_ssn_123");
        testUser.setPassHash("hashed_password_123");
        userRepo.save(testUser);
        
        // Create a test account
        testAccount = new accountsEntity();
        testAccount.setUserId(testUser);
        testAccount.setBalance(new BigDecimal("10000.00"));
        testAccount.setPortfolioSize(PortfolioSize.BALANCED);
        testAccount.setTradeType("stocks");
        accountsRepo.save(testAccount);
        
        // Create a test instrument
        testInstrument = new instrumentEntity();
        testInstrument.setTicker("AAPL");
        testInstrument.setAssetType("stock");
        testInstrument.setAssetName("Apple Inc.");
        testInstrument.setPrice(new BigDecimal("150.00"));
        testInstrument.setCurrency("USD");
        instrumentRepo.save(testInstrument);
        
        // Create a test order
        testOrder = new OrderEntity("BUY", testAccount, testInstrument, 100, new BigDecimal("15000.00"));
        testOrder.setStatus("PENDING");
        ordersRepo.save(testOrder);
    }
    
    @Test
    public void testInitialOrderCreatesSnapshot() throws Exception {
        // When an order is created, the trigger should capture a snapshot
        mockMvc.perform(get("/api/orders/" + testOrder.getOrderId() + "/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))  // At least 1 snapshot on INSERT
            .andExpect(jsonPath("$[0].order_information_json", containsString("PENDING")));
    }
    
    @Test
    public void testHistorySnapshotContainsOrderData() throws Exception {
        // Verify the JSONB snapshot has all order information
        mockMvc.perform(get("/api/orders/" + testOrder.getOrderId() + "/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].order_information_json", containsString("order_id")))
            .andExpect(jsonPath("$[0].order_information_json", containsString("side")))
            .andExpect(jsonPath("$[0].order_information_json", containsString("quantity")))
            .andExpect(jsonPath("$[0].order_information_json", containsString("total_price")))
            .andExpect(jsonPath("$[0].order_information_json", containsString("BUY")))
            .andExpect(jsonPath("$[0].order_information_json", containsString("status")));
    }
    
    @Test
    public void testHistorySnapshotHasTimestamp() throws Exception {
        // Verify the snapshot has a created_at timestamp
        mockMvc.perform(get("/api/orders/" + testOrder.getOrderId() + "/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].created_at", notNullValue()));
    }
    
    @Test
    public void testHistoryEndpointReturnsJsonArray() throws Exception {
        // Verify the endpoint returns valid JSON array
        mockMvc.perform(get("/api/orders/" + testOrder.getOrderId() + "/history")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", instanceOf(java.util.ArrayList.class)));
    }
    
    @Test
    public void testHistoryForNonexistentOrder() throws Exception {
        // Try to retrieve history for an order that doesn't exist
        mockMvc.perform(get("/api/orders/99999/history")
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
        List<historicalOrdersEntity> history = historicalOrdersRepo.findByOrderId_OrderIdOrderByCreatedAtAsc(testOrder.getOrderId());
        assert history.size() >= 1;
        assert history.get(0).getOrderInformationJson().contains("PENDING");
    }
}
