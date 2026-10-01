import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import main.Application;
import dto.InstrumentWithPrice;
import entities.AccountsEntity;
import repos.AccountsRepo;
import repos.InstrumentRepo;
import test.config.TestClockConfig;

import java.util.List;
import java.util.Optional;

/**
 * Soft-delete (accounts.account_active) and related account behaviour, verified end to end.
 *
 * Fixtures are inserted with raw SQL through JdbcTemplate, never through the repositories
 * under test, so a test can't pass merely because the writer and reader agree with each other.
 */
@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
public class AccountActiveFilteringTest {

    private static final int USER_ID = 900001;
    private static final int ACTIVE_ACCOUNT_ID = 900001;
    private static final int CLOSED_ACCOUNT_ID = 900002;
    private static final int ZERO_BALANCE_ACCOUNT_ID = 900003;
    private static final int NEVER_EXISTED_ACCOUNT_ID = 999999;
    private static final int PRICED_INSTRUMENT_ID = 900001;
    private static final int UNPRICED_INSTRUMENT_ID = 900002;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private InstrumentRepo instrumentRepo;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Filter Test', 'filter.test@example.com', DATE '1990-01-01', '1 Test St', 'SSN_HASH_SENTINEL', 'PASS_HASH_SENTINEL')",
            USER_ID);
        insertAccount(ACTIVE_ACCOUNT_ID, "5000.0000", true);
        insertAccount(CLOSED_ACCOUNT_ID, "0.0000", false);
        insertAccount(ZERO_BALANCE_ACCOUNT_ID, "0.0000", true);

        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'FLT_PRICED', 'STOCK', 'Priced Co', 'USD')",
            PRICED_INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, 10.0000, now(), now())",
            PRICED_INSTRUMENT_ID);
        // Deliberately no current_prices row: production has none of these today, so only a test can cover it
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) VALUES (?, 'FLT_UNPRICED', 'STOCK', 'Unpriced Co', 'USD')",
            UNPRICED_INSTRUMENT_ID);
    }

    private void insertAccount(int accountId, String balance, boolean active) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, CAST(? AS NUMERIC(18,4)), 'BALANCED', 'Passive', now(), ?)",
            accountId, USER_ID, balance, active);
    }

    // ---- findById / GET /accounts/{id} ----

    @Test
    void getActiveAccountReturnsData() throws Exception {
        mockMvc.perform(get("/accounts/" + ACTIVE_ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(ACTIVE_ACCOUNT_ID))
            .andExpect(jsonPath("$.account_active").value(true))
            .andExpect(jsonPath("$.balance").value(5000.0))
            .andExpect(jsonPath("$.userId").value(USER_ID))
            .andExpect(jsonPath("$.user.email").value("filter.test@example.com"));
    }

    @Test
    void getClosedAccountIsIndistinguishableFromMissing() throws Exception {
        String closed = mockMvc.perform(get("/accounts/" + CLOSED_ACCOUNT_ID))
            .andExpect(status().isNotFound())
            .andReturn().getResponse().getContentAsString();
        String missing = mockMvc.perform(get("/accounts/" + NEVER_EXISTED_ACCOUNT_ID))
            .andExpect(status().isNotFound())
            .andReturn().getResponse().getContentAsString();

        // Same body apart from the id and timestamp, so a caller can't tell "closed" from "never existed"
        assertEquals(normalise(missing, NEVER_EXISTED_ACCOUNT_ID), normalise(closed, CLOSED_ACCOUNT_ID));
    }

    private String normalise(String body, int id) throws Exception {
        JsonNode node = objectMapper.readTree(body);
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("timestamp");
        return node.toString().replace(String.valueOf(id), "{id}");
    }

    @Test
    void orderOnClosedAccountIsRejectedAndNothingIsWritten() throws Exception {
        String body = "{\"side\":\"BUY\",\"accountId\":" + CLOSED_ACCOUNT_ID
            + ",\"instrumentId\":" + PRICED_INSTRUMENT_ID + ",\"quantity\":1}";

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound());

        Integer orders = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM orders WHERE account_id = ?", Integer.class, CLOSED_ACCOUNT_ID);
        assertEquals(0, orders);
    }

    // ---- findAll / findByUser (no endpoint reads through these yet) ----

    @Test
    void findByUserExcludesClosedAccounts() {
        List<Integer> ids = accountsRepo.findByUser(USER_ID).stream().map(AccountsEntity::getAccountId).toList();
        assertTrue(ids.contains(ACTIVE_ACCOUNT_ID));
        assertFalse(ids.contains(CLOSED_ACCOUNT_ID), "closed account leaked through findByUser");
    }

    @Test
    void findAllExcludesClosedAccounts() {
        List<Integer> ids = accountsRepo.findAll().stream().map(AccountsEntity::getAccountId).toList();
        assertTrue(ids.contains(ACTIVE_ACCOUNT_ID));
        assertFalse(ids.contains(CLOSED_ACCOUNT_ID), "closed account leaked through findAll");
    }

    @Test
    void adminReadStillReturnsClosedAccount() {
        Optional<AccountsEntity> closed = accountsRepo.findByIdIncludingInactive(CLOSED_ACCOUNT_ID);
        assertTrue(closed.isPresent());
        assertFalse(closed.get().getAccountActive());
    }

    // ---- close ----

    @Test
    void closingTwiceGivesExplicitErrorSecondTime() throws Exception {
        String body = "{\"userId\":" + USER_ID + "}";

        mockMvc.perform(post("/accounts/close/" + ZERO_BALANCE_ACCOUNT_ID).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(content().string("Success"));

        Boolean active = jdbcTemplate.queryForObject(
            "SELECT account_active FROM accounts WHERE account_id = ?", Boolean.class, ZERO_BALANCE_ACCOUNT_ID);
        assertFalse(active);

        mockMvc.perform(post("/accounts/close/" + ZERO_BALANCE_ACCOUNT_ID).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Account is already closed"));
    }

    // ---- create ----

    @Test
    void createReturnsGeneratedIdOfTheStoredRow() throws Exception {
        String body = "{\"userId\":" + USER_ID + ",\"balance\":250.5,"
            + "\"portfolioSize\":\"LOW\",\"tradeType\":\"Active\"}";

        MvcResult result = mockMvc.perform(post("/accounts/create").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId", notNullValue()))
            .andReturn();

        int newId = objectMapper.readTree(result.getResponse().getContentAsString()).get("accountId").asInt();
        String tradeType = jdbcTemplate.queryForObject(
            "SELECT trade_type FROM accounts WHERE account_id = ? AND user_id = ?", String.class, newId, USER_ID);
        assertEquals("Active", tradeType);
    }

    @Test
    void createWithoutUserIdIsRejectedAndNothingIsWritten() throws Exception {
        Integer before = jdbcTemplate.queryForObject("SELECT count(*) FROM accounts", Integer.class);

        mockMvc.perform(post("/accounts/create").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":10,\"portfolioSize\":\"LOW\",\"tradeType\":\"Active\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldName").value("userId"));

        assertEquals(before, jdbcTemplate.queryForObject("SELECT count(*) FROM accounts", Integer.class));
    }

    @Test
    void closeWithoutUserIdIsRejected() throws Exception {
        mockMvc.perform(post("/accounts/close/" + ZERO_BALANCE_ACCOUNT_ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldName").value("userId"));

        Boolean active = jdbcTemplate.queryForObject(
            "SELECT account_active FROM accounts WHERE account_id = ?", Boolean.class, ZERO_BALANCE_ACCOUNT_ID);
        assertTrue(active);
    }

    // ---- positions take ids, not nested objects ----

    @Test
    void createPositionWithIdsStoresTheRow() throws Exception {
        String body = "{\"accountId\":" + ACTIVE_ACCOUNT_ID + ",\"instrumentId\":" + PRICED_INSTRUMENT_ID
            + ",\"quantity\":7,\"totalPrice\":70.00,\"averagePrice\":10.00}";

        mockMvc.perform(post("/positions/create").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());

        Integer quantity = jdbcTemplate.queryForObject(
            "SELECT quantity FROM positions WHERE account_id = ? AND instrument_id = ?",
            Integer.class, ACTIVE_ACCOUNT_ID, PRICED_INSTRUMENT_ID);
        assertEquals(7, quantity);
    }

    @Test
    void createPositionWithoutAccountIdIsRejectedAndNothingIsWritten() throws Exception {
        String body = "{\"instrumentId\":" + PRICED_INSTRUMENT_ID + ",\"quantity\":7,\"totalPrice\":70.00,\"averagePrice\":10.00}";

        mockMvc.perform(post("/positions/create").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldName").value("accountId"));

        Integer rows = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM positions WHERE instrument_id = ?", Integer.class, PRICED_INSTRUMENT_ID);
        assertEquals(0, rows);
    }

    // ---- instrument reads keep instruments that have no price (LEFT JOIN) ----

    @Test
    void instrumentWithoutPriceIsStillReadable() {
        Optional<InstrumentWithPrice> unpriced = instrumentRepo.findById(UNPRICED_INSTRUMENT_ID);
        assertTrue(unpriced.isPresent(), "instrument without a current_prices row was hidden");
        assertNull(unpriced.get().getPrice());
        assertNull(unpriced.get().getQuoteTime());
    }

    @Test
    void orderOnUnpricedInstrumentFailsWithMissingPriceNotNotFound() throws Exception {
        String body = "{\"side\":\"BUY\",\"accountId\":" + ACTIVE_ACCOUNT_ID
            + ",\"instrumentId\":" + UNPRICED_INSTRUMENT_ID + ",\"quantity\":1}";

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldMessage", containsString("no current price")));
    }
}
