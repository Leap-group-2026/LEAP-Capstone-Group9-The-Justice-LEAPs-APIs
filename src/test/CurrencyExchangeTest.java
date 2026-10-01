package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
//eeeeeeee
import main.Application;
import dto.request.TransactionRequest;
import entities.AccountsEntity;
import entities.InstrumentEntity;
import entities.UserEntity;
import entities.PortfolioSize;
import repos.AccountsRepo;
import repos.InstrumentRepo;
import repos.CurrentPriceRepo;
import repos.PositionsRepo;
import repos.UserRepo;
import test.config.TestClockConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("Currency Exchange Endpoint Tests")
public class CurrencyExchangeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private InstrumentRepo instrumentRepo;

    @Autowired
    private CurrentPriceRepo currentPriceRepo;

    @Autowired
    private PositionsRepo positionsRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private AccountsEntity testAccount;
    private InstrumentEntity eurInstrument;
    private InstrumentEntity gbpInstrument;

    @BeforeEach
    void setUp() {
        // Create test user
        UserEntity testUser = new UserEntity();
        testUser.setName("Exchange Test User");
        testUser.setEmail("exchange@test.com");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testUser.setAddress("123 Test Street");
        testUser.setSsnHash("test_ssn_hash");
        testUser.setPassHash("test_pass_hash");
        userRepo.insert(testUser);

        // Create test account with USD balance
        testAccount = new AccountsEntity();
        testAccount.setUserId(testUser);
        testAccount.setBalance(BigDecimal.valueOf(10000.00)); // $10,000 USD
        testAccount.setPortfolioSize(PortfolioSize.BALANCED);
        testAccount.setTradeType("ACTIVE");
        testAccount.setAccountActive(true);
        testAccount.setCreatedAt(LocalDateTime.now());
        accountsRepo.insert(testAccount);

        // Create USD/EUR instrument (service reverses EUR/USD to USD/EUR for lookups)
        eurInstrument = new InstrumentEntity();
        eurInstrument.setTicker("USD/EUR");
        eurInstrument.setAssetType("FOREX");
        eurInstrument.setAssetName("US Dollar to Euro");
        eurInstrument.setCurrency("EUR");
        instrumentRepo.insert(eurInstrument);

        // Create USD/GBP instrument (service reverses GBP/USD to USD/GBP for lookups)
        gbpInstrument = new InstrumentEntity();
        gbpInstrument.setTicker("USD/GBP");
        gbpInstrument.setAssetType("FOREX");
        gbpInstrument.setAssetName("US Dollar to British Pound");
        gbpInstrument.setCurrency("GBP");
        instrumentRepo.insert(gbpInstrument);

        // Set current prices for forex pairs using JdbcTemplate
        // CurrentPriceRepo.upsert uses Postgres-only ON CONFLICT, so use direct SQL
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, ?, now(), now())",
            eurInstrument.getInstrumentId(), BigDecimal.valueOf(1.10));
        
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, ?, now(), now())",
            gbpInstrument.getInstrumentId(), BigDecimal.valueOf(1.27));
    }

    // ============ SUCCESS CASES ============

    @Test
    @DisplayName("Successfully exchange USD for EUR with sufficient balance")
    void testSuccessfulUSDtoEURExchange() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "USD/EUR",
            BigDecimal.valueOf(500.00),
            "BUY",
            testAccount.getAccountId(),
            "EXCHANGE"
        );

        mockMvc.perform(post("/user/transactions/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Transaction processed successfully")));
    }

    @Test
    @DisplayName("Successfully exchange USD for GBP with sufficient balance")
    void testSuccessfulUSDtoGBPExchange() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "USD/GBP",
            BigDecimal.valueOf(1000.00),
            "BUY",
            testAccount.getAccountId(),
            "EXCHANGE"
        );

        mockMvc.perform(post("/user/transactions/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Transaction processed successfully")));
    }

    @Test
    @DisplayName("Successfully exchange small amount of USD for EUR")
    void testSuccessfulSmallAmountExchange() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "USD/EUR",
            BigDecimal.valueOf(50.00),
            "BUY",
            testAccount.getAccountId(),
            "EXCHANGE"
        );

        mockMvc.perform(post("/user/transactions/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Transaction processed successfully")));
    }

    // ============ FAILURE CASES ============

    @Test
    @DisplayName("Fail when insufficient USD balance")
    void testFailInsufficientBalance() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "USD/EUR",
            BigDecimal.valueOf(50000.00), // Requesting $50k when account only has $10k
            "BUY",
            testAccount.getAccountId(),
            "EXCHANGE"
        );

        mockMvc.perform(post("/user/transactions/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("You do not have enough balance in your account")));
    }

    @Test
    @DisplayName("Fail when account does not exist")
    void testFailAccountNotFound() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "USD/EUR",
            BigDecimal.valueOf(500.00),
            "BUY",
            9999, // Non-existent account ID
            "EXCHANGE"
        );

        mockMvc.perform(post("/user/transactions/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Account not found")));
    }

    @Test
    @DisplayName("Fail with invalid currency pair format")
    void testFailInvalidCurrencyPairFormat() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "USDERR", // Missing the "/" separator
            BigDecimal.valueOf(500.00),
            "BUY",
            testAccount.getAccountId(),
            "EXCHANGE"
        );

        mockMvc.perform(post("/user/transactions/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid exchange rate format")));
    }

    @Test
    @DisplayName("Fail when currency pair is blank")
    void testFailBlankCurrencyPair() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "", // Blank currency pair
            BigDecimal.valueOf(500.00),
            "BUY",
            testAccount.getAccountId(),
            "EXCHANGE"
        );

        mockMvc.perform(post("/user/transactions/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Base and exchange currency pair is required")));
    }
}
