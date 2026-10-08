import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import config.InternalApiKeyFilter;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import main.Application;
import config.JwtAuthenticationFilter;
import test.config.TestClockConfig;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * Who may call what, through the REAL security chain: JwtAuthenticationFilter plus SecurityConfig's rules.
 * Every other test imports TestSecurityConfig, which permits everything, so this is the only place a token
 * is actually checked. Tokens are signed the way the auth service signs them (HS256, the shared secret).
 *
 * Writes are checked in the database as well as by status code: a 404 that still moved money would pass a
 * status-only test. Each "cannot" test sits next to a "can" test for the same endpoint, so a failure can't
 * come from a broken fixture or a token the filter rejects for some other reason.
 */
@SpringBootTest(classes = Application.class, properties = "jwt.secret=" + SecurityTest.SECRET)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
public class SecurityTest {

    static final String SECRET = "security-test-secret-at-least-32-bytes-long-0123456789";

    private static final int OWNER_ID = 920101;      // the client making the requests
    private static final int OTHER_ID = 920102;      // another client, whose data must stay out of reach
    private static final int OWNER_ACCOUNT = 920101;
    private static final int OTHER_ACCOUNT = 920102;
    private static final int INSTRUMENT_ID = 920101;
    private static final int FOREX_ID = 920102;
    private static final int OTHER_ORDER = 920101;   // a PENDING order on the other client's account
    private static final int OWNER_EMPTY_ACCOUNT = 920103;  // zero balance, so it can be closed
    private static final int OTHER_EMPTY_ACCOUNT = 920104;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    @Value("${internal.api.key}")
    private String internalApiKey;

    @BeforeEach
    void setUp() {
        for (int userId : new int[] {OWNER_ID, OTHER_ID}) {
            jdbcTemplate.update(
                "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
                "VALUES (?, 'Security Test', ?, DATE '1990-01-01', '1 Test St', ?, 'pass')",
                userId, userId + "@security.test", "ssn-" + userId);
        }
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 10000.0000, 'BALANCED', 'Passive', now(), TRUE)", OWNER_ACCOUNT, OWNER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 10000.0000, 'BALANCED', 'Passive', now(), TRUE)", OTHER_ACCOUNT, OTHER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 0.0000, 'BALANCED', 'Passive', now(), TRUE)", OWNER_EMPTY_ACCOUNT, OWNER_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 0.0000, 'BALANCED', 'Passive', now(), TRUE)", OTHER_EMPTY_ACCOUNT, OTHER_ID);
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'SECTEST', 'STOCK', 'Security Test Co', 'USD')", INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, 10.0000, now(), now())",
            INSTRUMENT_ID);
        jdbcTemplate.update(
            "INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'USD/EUR', 'FOREX', 'US Dollar to Euro', 'EUR')", FOREX_ID);
        jdbcTemplate.update(
            "INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) VALUES (?, 1.1000, now(), now())",
            FOREX_ID);
        jdbcTemplate.update(
            "INSERT INTO orders (order_id, side, account_id, instrument_id, status, quantity, total_price) " +
            "VALUES (?, 'BUY', ?, ?, 'PENDING', 1, 10.0000)", OTHER_ORDER, OTHER_ACCOUNT, INSTRUMENT_ID);
    }

    // ---- Tokens ----

    private String token(String sub, String role, String type, Instant expiresAt, SecretKey signingKey) {
        return Jwts.builder()
            .subject(sub)
            .claim("role", role)
            .claim("type", type)
            .claim("sid", "security-test-session")
            .issuedAt(Date.from(expiresAt.minus(Duration.ofMinutes(30))))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    private String clientToken(int userId) {
        return token(String.valueOf(userId), "client", "access", Instant.now().plus(Duration.ofMinutes(30)), key);
    }

    private String adminToken(int adminId) {
        return token(String.valueOf(adminId), "admin", "access", Instant.now().plus(Duration.ofMinutes(30)), key);
    }

    private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String orderBody(int accountId) {
        return "{\"side\":\"BUY\",\"accountId\":" + accountId + ",\"instrumentId\":" + INSTRUMENT_ID + ",\"quantity\":1}";
    }

    private String exchangeBody(int accountId) {
        return "{\"baseAndExchange\":\"USD/EUR\",\"amount\":100,\"side\":\"BUY\",\"accountId\":" + accountId
            + ",\"transactionType\":\"EXCHANGE\"}";
    }

    private String positionBody(int accountId) {
        return "{\"accountId\":" + accountId + ",\"instrumentId\":" + INSTRUMENT_ID
            + ",\"quantity\":1000,\"totalPrice\":1.00,\"averagePrice\":0.001}";
    }

    private int count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }

    private boolean isActive(int accountId) {
        return jdbcTemplate.queryForObject("SELECT account_active FROM accounts WHERE account_id = ?", Boolean.class, accountId);
    }

    private BigDecimal balance(int accountId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, accountId);
    }

    // ---- The token is checked on every request ----

    @Test
    void aValidClientTokenOpensTheClientsOwnAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/accounts/" + OWNER_ACCOUNT))).andExpect(status().isOk());
    }

    @Test
    void noTokenGets401() throws Exception {
        mockMvc.perform(get("/accounts/" + OWNER_ACCOUNT)).andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredTokenGets401() throws Exception {
        String expired = token(String.valueOf(OWNER_ID), "client", "access", Instant.now().minusSeconds(60), key);

        mockMvc.perform(as(expired, get("/accounts/" + OWNER_ACCOUNT))).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenSignedWithAnotherSecretGets401() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor("a-different-secret-that-is-also-32-bytes-or-more!!".getBytes());
        String forged = token(String.valueOf(OWNER_ID), "client", "access", Instant.now().plusSeconds(600), otherKey);

        mockMvc.perform(as(forged, get("/accounts/" + OWNER_ACCOUNT))).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenThatIsNotAnAccessTokenGets401() throws Exception {
        String notAccess = token(String.valueOf(OWNER_ID), "client", "refresh", Instant.now().plusSeconds(600), key);

        mockMvc.perform(as(notAccess, get("/accounts/" + OWNER_ACCOUNT))).andExpect(status().isUnauthorized());
    }

    // The old fallback secret was too short for HS256, so every token failed and everyone looked logged out,
    // with nothing in the log. Starting must fail loudly instead.
    @Test
    void startupRefusesAMissingOrTooShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtAuthenticationFilter(""));
        assertThrows(IllegalStateException.class, () -> new JwtAuthenticationFilter("your-secret-key"));
        assertDoesNotThrow(() -> new JwtAuthenticationFilter(SECRET));
    }

    // ---- Login and registration need no JWT, so only the auth service, holding the internal key, may call them ----

    private static final String CREDENTIALS = "{\"email\":\"nobody@security.test\",\"password\":\"Wrong-Pass-123!!\"}";

    @Test
    void loginAndRegistrationWithoutTheInternalKeyGet401() throws Exception {
        for (String path : new String[] {"/user/login", "/admin/login", "/user"}) {
            mockMvc.perform(json(post(path), CREDENTIALS))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString(InternalApiKeyFilter.HEADER)));
        }
    }

    @Test
    void aWrongInternalKeyGets401() throws Exception {
        mockMvc.perform(json(post("/user/login"), CREDENTIALS).header(InternalApiKeyFilter.HEADER, internalApiKey + "x"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString(InternalApiKeyFilter.HEADER)));
    }

    // Past the filter the endpoint answers for itself: the login's own "Invalid email or password",
    // and registration's own validation error for an empty body
    @Test
    void theRightInternalKeyReachesTheEndpoint() throws Exception {
        mockMvc.perform(json(post("/user/login"), CREDENTIALS).header(InternalApiKeyFilter.HEADER, internalApiKey))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("Invalid email or password")));
        mockMvc.perform(json(post("/user"), "{}").header(InternalApiKeyFilter.HEADER, internalApiKey))
            .andExpect(status().isBadRequest());
    }

    // Only POST /user is registration; PATCH /user is a client updating their profile with their own token
    @Test
    void otherRequestsDoNotNeedTheInternalKey() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(patch("/user"), "{}")))
            .andExpect(content().string(not(containsString(InternalApiKeyFilter.HEADER))));
    }

    @Test
    void startupRefusesAMissingOrTooShortInternalKey() {
        assertThrows(IllegalStateException.class, () -> new InternalApiKeyFilter(""));
        assertThrows(IllegalStateException.class, () -> new InternalApiKeyFilter("too-short"));
        assertDoesNotThrow(() -> new InternalApiKeyFilter(internalApiKey));
    }

    // ---- Roles ----

    @Test
    void anAdminReachesAdminRoutes() throws Exception {
        mockMvc.perform(as(adminToken(1), get("/admin/accounts"))).andExpect(status().isOk());
    }

    @Test
    void aClientIsForbiddenFromAdminRoutes() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/admin/accounts"))).andExpect(status().isForbidden());
    }

    @Test
    void anAdminCanReadAnyClientsAccount() throws Exception {
        mockMvc.perform(as(adminToken(1), get("/accounts/" + OTHER_ACCOUNT))).andExpect(status().isOk());
    }

    // ---- A client only reaches their own data. "Not yours" answers 404, as checkAccountAccess already does,
    // so a client can't even learn that someone else's account or order exists ----

    @Test
    void aClientCannotReadAnotherClientsAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/accounts/" + OTHER_ACCOUNT))).andExpect(status().isNotFound());
    }

    @Test
    void aClientCanPlaceAnOrderOnTheirOwnAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/orders"), orderBody(OWNER_ACCOUNT))))
            .andExpect(status().isOk());

        assertEquals(1, count("SELECT count(*) FROM orders WHERE account_id = ?", OWNER_ACCOUNT));
    }

    @Test
    void aClientCannotPlaceAnOrderOnAnotherClientsAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/orders"), orderBody(OTHER_ACCOUNT))))
            .andExpect(status().isNotFound());

        assertEquals(1, count("SELECT count(*) FROM orders WHERE account_id = ?", OTHER_ACCOUNT),
            "an order was placed on someone else's account");
    }

    @Test
    void aClientCannotCancelAnotherClientsOrder() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), post("/orders/" + OTHER_ORDER + "/cancel")))
            .andExpect(status().isNotFound());

        assertEquals("PENDING", jdbcTemplate.queryForObject(
            "SELECT status FROM orders WHERE order_id = ?", String.class, OTHER_ORDER), "someone else's order was cancelled");
    }

    @Test
    void aClientCanCancelTheirOwnOrder() throws Exception {
        mockMvc.perform(as(clientToken(OTHER_ID), post("/orders/" + OTHER_ORDER + "/cancel"))).andExpect(status().isOk());
    }

    @Test
    void aClientCannotReadAnotherClientsOrderHistory() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/orders/" + OTHER_ORDER + "/history")))
            .andExpect(status().isNotFound());
    }

    @Test
    void aClientCanReadTheirOwnOrderHistory() throws Exception {
        mockMvc.perform(as(clientToken(OTHER_ID), get("/orders/" + OTHER_ORDER + "/history"))).andExpect(status().isOk());
    }

    @Test
    void aClientCannotListAnotherUsersAccounts() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/accounts/user/" + OTHER_ID))).andExpect(status().isNotFound());
    }

    @Test
    void aClientCanListTheirOwnAccounts() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/accounts/user/" + OWNER_ID))).andExpect(status().isOk());
    }

    @Test
    void aClientCanExchangeCurrencyOnTheirOwnAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/user/transactions/exchange"), exchangeBody(OWNER_ACCOUNT))))
            .andExpect(status().isOk());

        assertEquals(0, new BigDecimal("9900").compareTo(balance(OWNER_ACCOUNT)));
    }

    @Test
    void aClientCannotExchangeCurrencyOnAnotherClientsAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/user/transactions/exchange"), exchangeBody(OTHER_ACCOUNT))))
            .andExpect(status().isNotFound());

        assertEquals(0, new BigDecimal("10000").compareTo(balance(OTHER_ACCOUNT)),
            "someone else's balance moved: " + balance(OTHER_ACCOUNT));
    }

    // Positions should only come from fills: one created by hand at any price can be sold for real cash
    @Test
    void aClientCannotCreatePositionsEvenOnTheirOwnAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/positions"), positionBody(OWNER_ACCOUNT))))
            .andExpect(status().isForbidden());

        assertEquals(0, count("SELECT count(*) FROM positions WHERE account_id = ?", OWNER_ACCOUNT),
            "a client created a position by hand");
    }

    // ---- Admin and client ids come from different tables, so they overlap: admin 920101 and client 920101
    // carry the same sub. An admin's token must never act as the client with the same number ----

    private static final String NEW_ACCOUNT = "{\"balance\":100,\"portfolioSize\":\"BALANCED\",\"tradeType\":\"Passive\"}";

    @Test
    void aClientOpensAnAccountForThemselves() throws Exception {
        int before = count("SELECT count(*) FROM accounts WHERE user_id = ?", OWNER_ID);

        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/accounts"), NEW_ACCOUNT))).andExpect(status().isOk());

        assertEquals(before + 1, count("SELECT count(*) FROM accounts WHERE user_id = ?", OWNER_ID));
    }

    // Opening an account is for the caller, and an admin has no client record to own one
    @Test
    void anAdminCannotOpenAnAccountAndNeverForTheClientWithTheSameId() throws Exception {
        int before = count("SELECT count(*) FROM accounts WHERE user_id = ?", OWNER_ID);

        mockMvc.perform(as(adminToken(OWNER_ID), json(post("/accounts"), NEW_ACCOUNT))).andExpect(status().isForbidden());

        assertEquals(before, count("SELECT count(*) FROM accounts WHERE user_id = ?", OWNER_ID),
            "an admin's token opened an account in a client's name");
    }

    @Test
    void aClientClosesTheirOwnEmptyAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), patch("/accounts/close/" + OWNER_EMPTY_ACCOUNT))).andExpect(status().isOk());

        assertFalse(isActive(OWNER_EMPTY_ACCOUNT));
    }

    // Kept as it was: refused with the existing 400 "does not belong to you"
    @Test
    void aClientCannotCloseAnotherClientsAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), patch("/accounts/close/" + OTHER_EMPTY_ACCOUNT))).andExpect(status().isBadRequest());

        assertTrue(isActive(OTHER_EMPTY_ACCOUNT), "a client closed someone else's account");
    }

    // Admins can close any account, not just the one whose owner happens to share their id
    @Test
    void anAdminCanCloseAnyClientsAccount() throws Exception {
        mockMvc.perform(as(adminToken(1), patch("/accounts/close/" + OTHER_EMPTY_ACCOUNT))).andExpect(status().isOk());

        assertFalse(isActive(OTHER_EMPTY_ACCOUNT));
    }

    // Every other close rule still applies to admins
    @Test
    void anAdminCannotCloseAnAccountThatStillHasMoney() throws Exception {
        mockMvc.perform(as(adminToken(1), patch("/accounts/close/" + OTHER_ACCOUNT))).andExpect(status().isBadRequest());

        assertTrue(isActive(OTHER_ACCOUNT));
    }
}
