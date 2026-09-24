import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import main.Application;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.entities.UserEntity;
import main.entities.PortfolioSize;
import main.repos.AccountsRepo;
import main.repos.InstrumentRepo;
import main.repos.UserRepo;
import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("OrderController.createOrder() Tests")
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private InstrumentRepo instrumentRepo;

    @Autowired
    private UserRepo userRepo;

    private AccountsEntity testAccount;
    private InstrumentEntity testInstrument;

    @BeforeEach
    void setUp() {
        UserEntity testUser = new UserEntity();
        testUser.setName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testUser.setAddress("123 Test Street");
        testUser.setSsnHash("ssn_hash_value");
        testUser.setPassHash("pass_hash_value");
        userRepo.insert(testUser);
        // testUser.userId is now set by MyBatis via @Options

        testAccount = new AccountsEntity();
        testAccount.setUserId(testUser);
        testAccount.setBalance(BigDecimal.valueOf(20000.00));
        testAccount.setPortfolioSize(PortfolioSize.BALANCED);
        testAccount.setTradeType("ACTIVE");
        testAccount.setAccountActive(true);
        accountsRepo.insert(testUser.getUserId(), testAccount.getBalance(), 
                           testAccount.getPortfolioSize().getValue(), testAccount.getTradeType(), null, true);
        
        // Retrieve the created account to get its ID
        java.util.List<AccountsEntity> accounts = accountsRepo.findByUser(testUser.getUserId());
        if (!accounts.isEmpty()) {
            testAccount.setAccountId(accounts.get(0).getAccountId());
        }

        testInstrument = new InstrumentEntity();
        testInstrument.setTicker("AAPL");
        testInstrument.setAssetType("STOCK");
        testInstrument.setAssetName("Apple Inc.");
        testInstrument.setPrice(BigDecimal.valueOf(150.00));
        testInstrument.setCurrency("USD");
        instrumentRepo.insert(testInstrument);
        // testInstrument.instrumentId is now set by MyBatis via @Options
    }

    @Test
    @DisplayName("Should return 400 when 'side' field is missing")
    void testValidation_MissingSideField() throws Exception {
        String jsonPayload = "{\"accountId\": 1, \"instrumentId\": 1, \"quantity\": 100}";

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldName").value("side"));
    }

    @Test
    @DisplayName("Should return 400 when quantity is negative (violates @Positive)")
    void testValidation_NegativeQuantity() throws Exception {
        String jsonPayload = "{\"side\": \"BUY\", \"accountId\": 1, \"instrumentId\": 1, \"quantity\": -50}";

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.fieldName").value("quantity"))
            .andExpect(jsonPath("$.rejectedValue").value(-50));
    }

    @Test
    @DisplayName("Should return 400 when 'accountId' is missing")
    void testValidation_NullAccountId() throws Exception {
        String jsonPayload = "{\"side\": \"BUY\", \"instrumentId\": 1, \"quantity\": 100}";

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.fieldName").value("accountId"));
    }

    @Test
    @DisplayName("Should return 400 when 'instrumentId' is null (violates @NotNull)")
    void testValidation_NullInstrumentId() throws Exception {
        String jsonPayload = "{\"side\": \"BUY\", \"accountId\": 1, \"quantity\": 100}";

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldName").value("instrumentId"));
    }

    @Test
    @DisplayName("Should successfully create order with valid payload")
    void testCreateOrder_Success() throws Exception {
        String jsonPayload = "{\"side\": \"BUY\", \"accountId\": " + testAccount.getAccountId()
            + ", \"instrumentId\": " + testInstrument.getInstrumentId() + ", \"quantity\": 100}";

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.side").value("BUY"))
            .andExpect(jsonPath("$.quantity").value(100))
            .andExpect(jsonPath("$.total_price").value(15000.0));
    }
}
