import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import main.Application;
import test.config.TestClockConfig;
import test.config.TestSecurityConfig;

/**
 * Account names: PATCH /accounts/rename/{id}, the optional accountName on POST /accounts, and accountName in
 * every response that returns an account.
 *
 * Fixtures are inserted with raw SQL and names are read back with raw SQL, so a test can't pass just because
 * the code under test writes and reads the name the same wrong way. Each response test checks both a named
 * and an unnamed account, and that the unnamed one has the key with a null value rather than no key at all.
 */
@SpringBootTest(classes = Application.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
@Transactional
@DisplayName("Account name Tests")
public class AccountNameTest {

    private static final int OWNER_ID = 930101;
    private static final int OTHER_ID = 930102;
    private static final int NAMED_ACCOUNT = 930101;     // owner's, named "Savings"
    private static final int UNNAMED_ACCOUNT = 930102;   // owner's, no name
    private static final int FROZEN_ACCOUNT = 930103;    // owner's, frozen
    private static final int CLOSED_ACCOUNT = 930104;    // owner's, closed, named "Old"
    private static final int OTHER_ACCOUNT = 930105;     // the other client's, named "Theirs"
    private static final int MISSING_ACCOUNT = 939999;

    private static final String FORTY = "a".repeat(40);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        for (int userId : new int[] {OWNER_ID, OTHER_ID}) {
            jdbcTemplate.update(
                "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
                "VALUES (?, 'Name Test', ?, DATE '1990-01-01', '1 Test St', ?, 'PASS_HASH')",
                userId, userId + "@name.test", "SSN_HASH_" + userId);
        }
        insertAccount(NAMED_ACCOUNT, OWNER_ID, true, false, "Savings");
        insertAccount(UNNAMED_ACCOUNT, OWNER_ID, true, false, null);
        insertAccount(FROZEN_ACCOUNT, OWNER_ID, true, true, null);
        insertAccount(CLOSED_ACCOUNT, OWNER_ID, false, false, "Old");
        insertAccount(OTHER_ACCOUNT, OTHER_ID, true, false, "Theirs");
    }

    private void insertAccount(int accountId, int userId, boolean active, boolean frozen, String name) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active, account_frozen, account_name) " +
            "VALUES (?, ?, 1000.0000, 'BALANCED', 'Passive', now(), ?, ?, ?)",
            accountId, userId, active, frozen, name);
    }

    private String storedName(int accountId) {
        return jdbcTemplate.queryForObject("SELECT account_name FROM accounts WHERE account_id = ?", String.class, accountId);
    }

    private MockHttpServletRequestBuilder asClient(int userId, MockHttpServletRequestBuilder request) {
        return request.with(user(String.valueOf(userId)).roles("CLIENT"));
    }

    private ResultActions rename(int userId, int accountId, String body) throws Exception {
        return mockMvc.perform(asClient(userId, patch("/accounts/rename/" + accountId))
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    // The key must be there with a null value, not left out
    private void assertNullName(JsonNode account) {
        assertTrue(account.has("accountName"), "accountName key is missing: " + account);
        assertTrue(account.get("accountName").isNull(), "accountName should be null: " + account);
    }

    private JsonNode byId(JsonNode accounts, int accountId) {
        for (JsonNode account : accounts) {
            if (account.get("accountId").asInt() == accountId) {
                return account;
            }
        }
        fail("account " + accountId + " not in " + accounts);
        return null;
    }

    private void assertNamedAndUnnamed(JsonNode accounts) {
        assertEquals("Savings", byId(accounts, NAMED_ACCOUNT).get("accountName").asText());
        assertNullName(byId(accounts, UNNAMED_ACCOUNT));
    }

    // ---- PATCH /accounts/rename/{id} ----

    @Test
    void renameSetsTheName() throws Exception {
        JsonNode account = body(rename(OWNER_ID, UNNAMED_ACCOUNT, "{\"name\":\"Retirement\"}").andExpect(status().isOk()));

        assertEquals("Retirement", storedName(UNNAMED_ACCOUNT));
        assertEquals("Retirement", account.get("accountName").asText());
        assertEquals(UNNAMED_ACCOUNT, account.get("accountId").asInt());
    }

    // Same keys as GET /accounts/{id}, and only the name changed
    @Test
    void renameAnswersInTheShapeOfGetAccount() throws Exception {
        JsonNode before = body(mockMvc.perform(asClient(OWNER_ID, get("/accounts/" + NAMED_ACCOUNT))).andExpect(status().isOk()));
        JsonNode after = body(rename(OWNER_ID, NAMED_ACCOUNT, "{\"name\":\"Holiday\"}").andExpect(status().isOk()));

        ((com.fasterxml.jackson.databind.node.ObjectNode) before).put("accountName", "Holiday");
        assertEquals(before, after);
    }

    @Test
    void renameTrimsTheName() throws Exception {
        rename(OWNER_ID, UNNAMED_ACCOUNT, "{\"name\":\"   Savings  \"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountName").value("Savings"));

        assertEquals("Savings", storedName(UNNAMED_ACCOUNT));
    }

    @Test
    void renameWithNullClearsTheName() throws Exception {
        assertNullName(body(rename(OWNER_ID, NAMED_ACCOUNT, "{\"name\":null}").andExpect(status().isOk())));

        assertNull(storedName(NAMED_ACCOUNT));
    }

    @Test
    void renameWithNoNameFieldClearsTheName() throws Exception {
        rename(OWNER_ID, NAMED_ACCOUNT, "{}").andExpect(status().isOk());

        assertNull(storedName(NAMED_ACCOUNT));
    }

    @Test
    void renameWithAnEmptyStringClearsTheName() throws Exception {
        assertNullName(body(rename(OWNER_ID, NAMED_ACCOUNT, "{\"name\":\"\"}").andExpect(status().isOk())));

        assertNull(storedName(NAMED_ACCOUNT));
    }

    @Test
    void renameWithOnlySpacesClearsTheName() throws Exception {
        rename(OWNER_ID, NAMED_ACCOUNT, "{\"name\":\"    \"}").andExpect(status().isOk());

        assertNull(storedName(NAMED_ACCOUNT));
    }

    @Test
    void renameAcceptsFortyCharacters() throws Exception {
        rename(OWNER_ID, UNNAMED_ACCOUNT, "{\"name\":\"" + FORTY + "\"}").andExpect(status().isOk());

        assertEquals(FORTY, storedName(UNNAMED_ACCOUNT));
    }

    // The limit applies after trimming
    @Test
    void renameAcceptsFortyCharactersWithSurroundingSpaces() throws Exception {
        rename(OWNER_ID, UNNAMED_ACCOUNT, "{\"name\":\"  " + FORTY + "  \"}").andExpect(status().isOk());

        assertEquals(FORTY, storedName(UNNAMED_ACCOUNT));
    }

    // Characters, not UTF-16 units: 40 emoji are 80 chars in Java but 40 characters
    @Test
    void renameCountsCharactersNotUtf16Units() throws Exception {
        String emoji = "💰".repeat(40);
        rename(OWNER_ID, UNNAMED_ACCOUNT, "{\"name\":\"" + emoji + "\"}").andExpect(status().isOk());

        assertEquals(emoji, storedName(UNNAMED_ACCOUNT));
    }

    @Test
    void renameRejectsFortyOneCharacters() throws Exception {
        rename(OWNER_ID, NAMED_ACCOUNT, "{\"name\":\"" + FORTY + "b\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldName").value("name"))
            .andExpect(jsonPath("$.rejectedValue").value(FORTY + "b"))
            .andExpect(jsonPath("$.fieldMessage").value("Account name must be at most 40 characters"));

        assertEquals("Savings", storedName(NAMED_ACCOUNT));
    }

    @Test
    void renameRejectsControlCharacters() throws Exception {
        for (String name : new String[] {"Sav\\u0007ings", "Sav\\nings", "Savings\\t", "Sav\\u0085ings", "Sav\\u007fings"}) {
            rename(OWNER_ID, NAMED_ACCOUNT, "{\"name\":\"" + name + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldName").value("name"))
                .andExpect(jsonPath("$.fieldMessage").value("Account name must not contain control characters"));
        }

        assertEquals("Savings", storedName(NAMED_ACCOUNT));
    }

    @Test
    void namesNeedNotBeUnique() throws Exception {
        rename(OWNER_ID, UNNAMED_ACCOUNT, "{\"name\":\"Savings\"}").andExpect(status().isOk());

        assertEquals("Savings", storedName(UNNAMED_ACCOUNT));
        assertEquals("Savings", storedName(NAMED_ACCOUNT));
    }

    @Test
    void renamingAnotherClientsAccountIs404() throws Exception {
        rename(OWNER_ID, OTHER_ACCOUNT, "{\"name\":\"Mine now\"}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.fieldName").value("Account"))
            .andExpect(jsonPath("$.rejectedValue").value(String.valueOf(OTHER_ACCOUNT)));

        assertEquals("Theirs", storedName(OTHER_ACCOUNT));
    }

    @Test
    void renamingAClosedAccountIs404() throws Exception {
        rename(OWNER_ID, CLOSED_ACCOUNT, "{\"name\":\"Reopened\"}").andExpect(status().isNotFound());

        assertEquals("Old", storedName(CLOSED_ACCOUNT));
    }

    @Test
    void renamingAMissingAccountIs404() throws Exception {
        rename(OWNER_ID, MISSING_ACCOUNT, "{\"name\":\"Nothing\"}").andExpect(status().isNotFound());
    }

    @Test
    void aFrozenAccountCanBeRenamed() throws Exception {
        rename(OWNER_ID, FROZEN_ACCOUNT, "{\"name\":\"On hold\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account_frozen").value(true))
            .andExpect(jsonPath("$.accountName").value("On hold"));

        assertEquals("On hold", storedName(FROZEN_ACCOUNT));
    }

    @Test
    void anAdminCannotRename() throws Exception {
        mockMvc.perform(patch("/accounts/rename/" + OTHER_ACCOUNT).with(user("1").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Admin's\"}"))
            .andExpect(status().isForbidden());

        assertEquals("Theirs", storedName(OTHER_ACCOUNT));
    }

    // ---- POST /accounts ----

    @Test
    void createWithAName() throws Exception {
        JsonNode account = body(mockMvc.perform(asClient(OWNER_ID, post("/accounts"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":100,\"portfolioSize\":\"LOW\",\"tradeType\":\"Active\",\"accountName\":\"  Kids  \"}"))
            .andExpect(status().isOk()));

        assertEquals("Kids", account.get("accountName").asText());
        assertEquals("Kids", storedName(account.get("accountId").asInt()));
    }

    @Test
    void createWithoutAName() throws Exception {
        JsonNode account = body(mockMvc.perform(asClient(OWNER_ID, post("/accounts"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":100,\"portfolioSize\":\"LOW\",\"tradeType\":\"Active\"}"))
            .andExpect(status().isOk()));

        assertNullName(account);
        assertNull(storedName(account.get("accountId").asInt()));
    }

    @Test
    void createWithABlankNameStoresNoName() throws Exception {
        JsonNode account = body(mockMvc.perform(asClient(OWNER_ID, post("/accounts"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":100,\"portfolioSize\":\"LOW\",\"tradeType\":\"Active\",\"accountName\":\"  \"}"))
            .andExpect(status().isOk()));

        assertNullName(account);
        assertNull(storedName(account.get("accountId").asInt()));
    }

    @Test
    void createRejectsAnInvalidName() throws Exception {
        int before = jdbcTemplate.queryForObject("SELECT count(*) FROM accounts WHERE user_id = ?", Integer.class, OWNER_ID);

        mockMvc.perform(asClient(OWNER_ID, post("/accounts"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":100,\"portfolioSize\":\"LOW\",\"tradeType\":\"Active\",\"accountName\":\"" + FORTY + "b\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldName").value("accountName"));

        assertEquals(before, jdbcTemplate.queryForObject("SELECT count(*) FROM accounts WHERE user_id = ?", Integer.class, OWNER_ID));
    }

    // ---- accountName in every response that returns an account ----

    @Test
    void getAccountIncludesTheName() throws Exception {
        mockMvc.perform(asClient(OWNER_ID, get("/accounts/" + NAMED_ACCOUNT)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountName").value("Savings"));
        assertNullName(body(mockMvc.perform(asClient(OWNER_ID, get("/accounts/" + UNNAMED_ACCOUNT))).andExpect(status().isOk())));
    }

    @Test
    void accountsByUserIdIncludeTheName() throws Exception {
        assertNamedAndUnnamed(body(mockMvc.perform(asClient(OWNER_ID, get("/accounts/user/" + OWNER_ID))).andExpect(status().isOk())));
    }

    @Test
    void currentUsersAccountsIncludeTheName() throws Exception {
        assertNamedAndUnnamed(body(mockMvc.perform(asClient(OWNER_ID, get("/user/accounts"))).andExpect(status().isOk())));
    }

    @Test
    void adminAccountListsIncludeTheName() throws Exception {
        assertNamedAndUnnamed(body(mockMvc.perform(get("/admin/accounts")).andExpect(status().isOk())));
        assertNamedAndUnnamed(body(mockMvc.perform(get("/admin/accounts/" + OWNER_ID)).andExpect(status().isOk())));
    }

    @Test
    void adminSingleAccountIncludesTheName() throws Exception {
        mockMvc.perform(get("/admin/accounts/account/" + NAMED_ACCOUNT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountName").value("Savings"));
        assertNullName(body(mockMvc.perform(get("/admin/accounts/account/" + UNNAMED_ACCOUNT)).andExpect(status().isOk())));
        // Admins see closed accounts too, name included
        mockMvc.perform(get("/admin/accounts/account/" + CLOSED_ACCOUNT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountName").value("Old"));
    }

    @Test
    void adminUserLookupIncludesTheName() throws Exception {
        assertNamedAndUnnamed(body(mockMvc.perform(get("/admin/users/" + OWNER_ID)).andExpect(status().isOk())).get("accounts"));
    }
}
