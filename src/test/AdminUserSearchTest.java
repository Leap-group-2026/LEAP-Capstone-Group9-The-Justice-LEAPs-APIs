import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import main.Application;
import test.config.TestClockConfig;
import test.config.TestSecurityConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin search endpoints: GET /admin/users, GET /admin/users/{id} and GET /admin/accounts/account/{accountId}
 *
 * Fixtures are inserted directly with JdbcTemplate and read back through the endpoint, so the
 * test exercises the real SQL and result mapping rather than agreeing with itself.
 */
@SpringBootTest(classes = Application.class)
@Import({TestClockConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
@Transactional
@DisplayName("Admin User and Account Search Endpoint Tests")
public class AdminUserSearchTest {

    private static final int USER_1_ID = 900901;
    private static final int USER_2_ID = 900902;
    private static final int USER_3_ID = 900903;
    private static final int NONEXISTENT_ID = 999999;

    private static final int ACTIVE_ACCOUNT = 900901;
    private static final int SECOND_ACTIVE_ACCOUNT = 900902;
    private static final int CLOSED_ACCOUNT = 900903;
    private static final int FROZEN_ACCOUNT = 900904;
    private static final int OTHER_USERS_ACCOUNT = 900905;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Inserted out of id order so the response's ordering has to come from the query
        insertUser(USER_3_ID, "Search User 3", "search.user3@example.com", "1985-03-03", "3 Third St", null);
        insertUser(USER_1_ID, "Search User 1", "search.user1@example.com", "1990-01-01", "1 First St", "123456");
        insertUser(USER_2_ID, "Search User 2", "search.user2@example.com", "1975-12-31", "2 Second St", "654321");

        insertAccount(ACTIVE_ACCOUNT, USER_1_ID, "1234.5600", "BALANCED", "Stocks", "2026-09-01 10:00:00", true, false);
        insertAccount(SECOND_ACTIVE_ACCOUNT, USER_1_ID, "50.0000", "HIGH", "Options", "2026-09-02 11:00:00", true, false);
        insertAccount(CLOSED_ACCOUNT, USER_1_ID, "0.0000", "LOW", "Passive", "2026-09-03 12:00:00", false, false);
        insertAccount(FROZEN_ACCOUNT, USER_1_ID, "777.7700", "HIGH", "Crypto", "2026-09-04 13:00:00", true, true);
        insertAccount(OTHER_USERS_ACCOUNT, USER_2_ID, "10.0000", "LOW", "Stocks", "2026-09-05 14:00:00", true, false);
    }

    private void insertUser(int userId, String name, String email, String dateOfBirth, String address, String code) {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash, code) " +
            "VALUES (?, ?, ?, CAST(? AS DATE), ?, ?, ?, ?)",
            userId, name, email, dateOfBirth, address, "SSN_HASH_" + userId, "PASS_HASH_" + userId, code);
    }

    private void insertAccount(int accountId, int userId, String balance, String portfolioSize, String tradeType,
                               String createdAt, boolean active, boolean frozen) {
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active, account_frozen) " +
            "VALUES (?, ?, CAST(? AS NUMERIC(18,4)), ?, ?, CAST(? AS TIMESTAMP), ?, ?)",
            accountId, userId, balance, portfolioSize, tradeType, createdAt, active, frozen);
    }

    private JsonNode getJson(String path) throws Exception {
        String body = mockMvc.perform(get(path))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static Set<String> keys(JsonNode node) {
        Set<String> keys = new HashSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    // ---- GET /admin/users ----

    @Test
    @DisplayName("GET /admin/users - Every user has exactly the five safe keys, and no hash or reset code")
    void usersHaveOnlyTheSafeKeys() throws Exception {
        JsonNode users = getJson("/admin/users");

        assertTrue(users.size() >= 3);
        for (JsonNode user : users) {
            assertEquals(Set.of("userId", "name", "email", "dateOfBirth", "address"), keys(user),
                "unexpected keys on user " + user.get("userId"));
        }
        // Belt and braces: the secret values themselves must not appear anywhere in the body
        String body = users.toString();
        for (String secret : new String[] {"PASS_HASH_", "SSN_HASH_", "123456", "654321"}) {
            assertFalse(body.contains(secret), "response leaks " + secret);
        }
    }

    @Test
    @DisplayName("GET /admin/users - Should map every column to the value stored in the database")
    void usersMapEveryFieldFromTheDatabaseRow() throws Exception {
        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.userId == " + USER_1_ID + ")].name").value(contains("Search User 1")))
            .andExpect(jsonPath("$[?(@.userId == " + USER_1_ID + ")].email").value(contains("search.user1@example.com")))
            .andExpect(jsonPath("$[?(@.userId == " + USER_1_ID + ")].dateOfBirth").value(contains("1990-01-01")))
            .andExpect(jsonPath("$[?(@.userId == " + USER_1_ID + ")].address").value(contains("1 First St")))
            .andExpect(jsonPath("$[?(@.userId == " + USER_2_ID + ")].dateOfBirth").value(contains("1975-12-31")))
            .andExpect(jsonPath("$[?(@.userId == " + USER_3_ID + ")].name").value(contains("Search User 3")));
    }

    @Test
    @DisplayName("GET /admin/users - Should be sorted by userId ascending")
    void usersAreSortedByUserId() throws Exception {
        List<Integer> ids = new ArrayList<>();
        getJson("/admin/users").forEach(user -> ids.add(user.get("userId").asInt()));

        List<Integer> sorted = new ArrayList<>(ids);
        sorted.sort(null);
        assertEquals(sorted, ids);
        assertTrue(ids.indexOf(USER_1_ID) < ids.indexOf(USER_2_ID) && ids.indexOf(USER_2_ID) < ids.indexOf(USER_3_ID));
    }

    // ---- GET /admin/users/{id} ----

    @Test
    @DisplayName("GET /admin/users/{id} - Should return the user and their active accounts, frozen included")
    void userByIdReturnsTheUserWithTheirAccounts() throws Exception {
        mockMvc.perform(get("/admin/users/" + USER_1_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_1_ID))
            .andExpect(jsonPath("$.name").value("Search User 1"))
            .andExpect(jsonPath("$.email").value("search.user1@example.com"))
            .andExpect(jsonPath("$.dateOfBirth").value("1990-01-01"))
            .andExpect(jsonPath("$.address").value("1 First St"))
            .andExpect(jsonPath("$.accounts", hasSize(3)))
            .andExpect(jsonPath("$.accounts[*].accountId",
                containsInAnyOrder(ACTIVE_ACCOUNT, SECOND_ACTIVE_ACCOUNT, FROZEN_ACCOUNT)))
            .andExpect(jsonPath("$.accounts[*].userId", everyItem(is(USER_1_ID))))
            // Same as /admin/users/by-email: the lookup lists active accounts only, so the closed one is left out
            .andExpect(jsonPath("$.accounts[*].accountId", not(hasItem(CLOSED_ACCOUNT))))
            .andExpect(jsonPath("$.accounts[*].accountId", not(hasItem(OTHER_USERS_ACCOUNT))))
            .andExpect(jsonPath("$.accounts[?(@.accountId == " + FROZEN_ACCOUNT + ")].accountFrozen").value(contains(true)))
            .andExpect(jsonPath("$.accounts[?(@.accountId == " + ACTIVE_ACCOUNT + ")].balance").value(contains(1234.56)));
    }

    @Test
    @DisplayName("GET /admin/users/{id} - Should not include a hash or reset code")
    void userByIdHasOnlyTheSafeKeys() throws Exception {
        JsonNode user = getJson("/admin/users/" + USER_1_ID);

        assertEquals(Set.of("userId", "name", "email", "dateOfBirth", "address", "accounts"), keys(user));
        assertFalse(user.toString().contains("PASS_HASH_") || user.toString().contains("SSN_HASH_")
            || user.toString().contains("123456"));
    }

    @Test
    @DisplayName("GET /admin/users/{id} - Should return the same body as GET /admin/users/by-email")
    void userByIdMatchesLookupByEmail() throws Exception {
        assertEquals(getJson("/admin/users/by-email?email=search.user1@example.com"), getJson("/admin/users/" + USER_1_ID));
    }

    @Test
    @DisplayName("GET /admin/users/{id} - Should return an empty accounts list for a user with no accounts")
    void userByIdWithNoAccounts() throws Exception {
        mockMvc.perform(get("/admin/users/" + USER_3_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_3_ID))
            .andExpect(jsonPath("$.accounts", hasSize(0)));
    }

    @Test
    @DisplayName("GET /admin/users/{id} - Should return 404 ValidationError for an unknown user")
    void userByIdReturns404ForUnknownUser() throws Exception {
        mockMvc.perform(get("/admin/users/" + NONEXISTENT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("User with ID " + NONEXISTENT_ID + " not found"))
            .andExpect(jsonPath("$.fieldName").value("User"))
            .andExpect(jsonPath("$.rejectedValue").value(String.valueOf(NONEXISTENT_ID)))
            .andExpect(jsonPath("$.fieldMessage").value("Resource not found"));
    }

    @Test
    @DisplayName("GET /admin/users/by-email - Still resolves to the email route, not /admin/users/{id}")
    void byEmailIsNotCapturedByTheIdRoute() throws Exception {
        mockMvc.perform(get("/admin/users/by-email").param("email", "search.user2@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_2_ID));
    }

    // ---- GET /admin/accounts/account/{accountId} ----

    @Test
    @DisplayName("GET /admin/accounts/account/{accountId} - Should map every field of an active account")
    void accountReturnsAnActiveAccount() throws Exception {
        mockMvc.perform(get("/admin/accounts/account/" + ACTIVE_ACCOUNT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(ACTIVE_ACCOUNT))
            .andExpect(jsonPath("$.userId").value(USER_1_ID))
            .andExpect(jsonPath("$.balance").value(1234.56))
            .andExpect(jsonPath("$.portfolioSize").value("BALANCED"))
            .andExpect(jsonPath("$.tradeType").value("Stocks"))
            .andExpect(jsonPath("$.createdAt").value(startsWith("2026-09-01T10:00")))
            .andExpect(jsonPath("$.accountActive").value(true))
            .andExpect(jsonPath("$.accountFrozen").value(false));
    }

    @Test
    @DisplayName("GET /admin/accounts/account/{accountId} - Should return a closed account")
    void accountReturnsAClosedAccount() throws Exception {
        mockMvc.perform(get("/admin/accounts/account/" + CLOSED_ACCOUNT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(CLOSED_ACCOUNT))
            .andExpect(jsonPath("$.userId").value(USER_1_ID))
            .andExpect(jsonPath("$.balance").value(0.0))
            .andExpect(jsonPath("$.portfolioSize").value("LOW"))
            .andExpect(jsonPath("$.tradeType").value("Passive"))
            .andExpect(jsonPath("$.createdAt").value(startsWith("2026-09-03T12:00")))
            .andExpect(jsonPath("$.accountActive").value(false))
            .andExpect(jsonPath("$.accountFrozen").value(false));
    }

    @Test
    @DisplayName("GET /admin/accounts/account/{accountId} - Should return a frozen account")
    void accountReturnsAFrozenAccount() throws Exception {
        mockMvc.perform(get("/admin/accounts/account/" + FROZEN_ACCOUNT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(FROZEN_ACCOUNT))
            .andExpect(jsonPath("$.userId").value(USER_1_ID))
            .andExpect(jsonPath("$.balance").value(777.77))
            .andExpect(jsonPath("$.portfolioSize").value("HIGH"))
            .andExpect(jsonPath("$.tradeType").value("Crypto"))
            .andExpect(jsonPath("$.createdAt").value(startsWith("2026-09-04T13:00")))
            .andExpect(jsonPath("$.accountActive").value(true))
            .andExpect(jsonPath("$.accountFrozen").value(true));
    }

    @Test
    @DisplayName("GET /admin/accounts/account/{accountId} - Should embed no user details")
    void accountHasOnlyTheAccountKeys() throws Exception {
        JsonNode account = getJson("/admin/accounts/account/" + ACTIVE_ACCOUNT);

        assertEquals(Set.of("accountId", "userId", "balance", "portfolioSize", "tradeType", "createdAt",
            "accountActive", "accountFrozen", "accountName"), keys(account));
    }

    @Test
    @DisplayName("GET /admin/accounts/account/{accountId} - Should return 404 ValidationError for an unknown account")
    void accountReturns404ForUnknownAccount() throws Exception {
        mockMvc.perform(get("/admin/accounts/account/" + NONEXISTENT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Account with ID " + NONEXISTENT_ID + " not found"))
            .andExpect(jsonPath("$.fieldName").value("Account"))
            .andExpect(jsonPath("$.rejectedValue").value(String.valueOf(NONEXISTENT_ID)))
            .andExpect(jsonPath("$.fieldMessage").value("Resource not found"));
    }

    @Test
    @DisplayName("GET /admin/accounts/{userId} - Still lists a user's accounts, not captured by the new route")
    void accountsByUserIsUnaffected() throws Exception {
        mockMvc.perform(get("/admin/accounts/" + USER_2_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].accountId").value(OTHER_USERS_ACCOUNT));
    }
}
