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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import main.Application;
import config.JwtAuthenticationFilter;
import test.config.TestClockConfig;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    private static final int ADMIN_ID = 920105;      // an admin whose number matches no client here

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private final Map<String, UUID> sessions = new HashMap<>();

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
        for (int adminId : new int[] {OWNER_ID, ADMIN_ID}) {
            jdbcTemplate.update("INSERT INTO admin (admin_id, email, pass_hash, role) VALUES (?, ?, 'pass', 'ADMIN')",
                adminId, "admin" + adminId + "@security.test");
        }
        for (int userId : new int[] {OWNER_ID, OTHER_ID}) {
            sessions.put("client:" + userId, insertSession("client", userId));
        }
        for (int adminId : new int[] {OWNER_ID, ADMIN_ID}) {
            sessions.put("admin:" + adminId, insertSession("admin", adminId));
        }
    }

    private Instant now() {
        return clock.instant();
    }

    private UUID insertSession(String role, int ownerId) {
        return insertSession(role, ownerId, now(), now().plus(Duration.ofHours(8)));
    }

    private UUID insertSession(String role, int ownerId, Instant lastActivity, Instant expiresAt) {
        UUID sid = UUID.randomUUID();
        boolean client = "client".equals(role);
        jdbcTemplate.update(
            "INSERT INTO user_sessions (session_id, user_id, admin_id, role, created_at, last_activity_at, expires_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            sid, client ? ownerId : null, client ? null : ownerId, role,
            Timestamp.from(now().minus(Duration.ofHours(1))), Timestamp.from(lastActivity), Timestamp.from(expiresAt));
        return sid;
    }

    private Instant lastActivity(UUID sid) {
        return jdbcTemplate.queryForObject("SELECT last_activity_at FROM user_sessions WHERE session_id = ?",
            Timestamp.class, sid).toInstant();
    }

    // ---- Tokens ----

    // exp uses real time (jjwt checks it); session rows use the fixed test clock
    private String token(String sub, String role, String type, Object sid, Instant expiresAt, SecretKey signingKey) {
        var builder = Jwts.builder()
            .subject(sub)
            .claim("role", role)
            .claim("type", type)
            .issuedAt(Date.from(expiresAt.minus(Duration.ofMinutes(30))))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey, Jwts.SIG.HS256);
        if (sid != null) {
            builder.claim("sid", sid.toString());
        }
        return builder.compact();
    }

    private String token(String sub, String role, String type, Instant expiresAt, SecretKey signingKey) {
        return token(sub, role, type, sessions.get(role + ":" + sub), expiresAt, signingKey);
    }

    private String clientToken(int userId) {
        return clientToken(userId, sessions.get("client:" + userId));
    }

    private String clientToken(int userId, UUID sid) {
        return token(String.valueOf(userId), "client", "access", sid, Instant.now().plus(Duration.ofMinutes(30)), key);
    }

    private String adminToken(int adminId) {
        return token(String.valueOf(adminId), "admin", "access", sessions.get("admin:" + adminId),
            Instant.now().plus(Duration.ofMinutes(30)), key);
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
        expect401(get("/accounts/" + OWNER_ACCOUNT), "unauthenticated");
    }

    @Test
    void anExpiredTokenGets401() throws Exception {
        String expired = token(String.valueOf(OWNER_ID), "client", "access", Instant.now().minusSeconds(60), key);

        expect401(as(expired, get("/accounts/" + OWNER_ACCOUNT)), "token_expired");
    }

    @Test
    void aTokenSignedWithAnotherSecretGets401() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor("a-different-secret-that-is-also-32-bytes-or-more!!".getBytes());
        String forged = token(String.valueOf(OWNER_ID), "client", "access", Instant.now().plusSeconds(600), otherKey);

        expect401(as(forged, get("/accounts/" + OWNER_ACCOUNT)), "token_invalid");
    }

    @Test
    void aTokenThatIsNotAnAccessTokenGets401() throws Exception {
        String notAccess = token(String.valueOf(OWNER_ID), "client", "refresh", Instant.now().plusSeconds(600), key);

        expect401(as(notAccess, get("/accounts/" + OWNER_ACCOUNT)), "token_invalid");
    }

    private void expect401(MockHttpServletRequestBuilder request, String code) throws Exception {
        mockMvc.perform(request)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value(code))
            .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void aMalformedAuthorizationHeaderIsUnauthenticated() throws Exception {
        expect401(get("/accounts/" + OWNER_ACCOUNT).header("Authorization", "Basic dXNlcjpwYXNz"), "unauthenticated");
        expect401(get("/accounts/" + OWNER_ACCOUNT).header("Authorization", "Bearer "), "unauthenticated");
    }

    @Test
    void aTokenWithoutASidIsInvalid() throws Exception {
        String noSid = token(String.valueOf(OWNER_ID), "client", "access", null, Instant.now().plusSeconds(600), key);

        expect401(as(noSid, get("/accounts/" + OWNER_ACCOUNT)), "token_invalid");
    }

    @Test
    void aLiveSessionOpensTheRouteAndRecordsActivityOnlyAfterSixtySeconds() throws Exception {
        UUID recent = insertSession("client", OWNER_ID, now().minusSeconds(59), now().plus(Duration.ofHours(1)));
        UUID stale = insertSession("client", OWNER_ID, now().minusSeconds(61), now().plus(Duration.ofHours(1)));

        mockMvc.perform(as(clientToken(OWNER_ID, recent), get("/accounts/" + OWNER_ACCOUNT))).andExpect(status().isOk());
        mockMvc.perform(as(clientToken(OWNER_ID, stale), get("/accounts/" + OWNER_ACCOUNT))).andExpect(status().isOk());

        assertEquals(now().minusSeconds(59), lastActivity(recent), "activity was written within 60 s of the last write");
        assertEquals(now(), lastActivity(stale));
    }

    @Test
    void aRevokedSessionGets401SessionRevoked() throws Exception {
        UUID sid = sessions.get("client:" + OWNER_ID);
        jdbcTemplate.update("UPDATE user_sessions SET revoked_at = ?, revoked_reason = 'logout' WHERE session_id = ?",
            Timestamp.from(now()), sid);

        expect401(as(clientToken(OWNER_ID), get("/accounts/" + OWNER_ACCOUNT)), "session_revoked");
    }

    @Test
    void aSessionPastItsCapGets401SessionExpired() throws Exception {
        UUID sid = insertSession("client", OWNER_ID, now(), now());

        expect401(as(clientToken(OWNER_ID, sid), get("/accounts/" + OWNER_ACCOUNT)), "session_expired");
    }

    @Test
    void aSessionIdleFor31MinutesGets401SessionIdle() throws Exception {
        UUID sid = insertSession("client", OWNER_ID, now().minus(Duration.ofMinutes(31)), now().plus(Duration.ofHours(1)));

        expect401(as(clientToken(OWNER_ID, sid), get("/accounts/" + OWNER_ACCOUNT)), "session_idle");
    }

    @Test
    void anUnknownSidGets401SessionInvalid() throws Exception {
        expect401(as(clientToken(OWNER_ID, UUID.randomUUID()), get("/accounts/" + OWNER_ACCOUNT)), "session_invalid");
    }

    @Test
    void anAdminTokenWithAClientsSidGets401SessionInvalid() throws Exception {
        String adminWithClientSid = token(String.valueOf(OWNER_ID), "admin", "access", sessions.get("client:" + OWNER_ID),
            Instant.now().plusSeconds(600), key);

        expect401(as(adminWithClientSid, get("/admin/accounts")), "session_invalid");
    }

    @Test
    void aClientTokenWithAnotherClientsSidGets401SessionInvalid() throws Exception {
        expect401(as(clientToken(OWNER_ID, sessions.get("client:" + OTHER_ID)), get("/accounts/" + OWNER_ACCOUNT)), "session_invalid");
    }

    @Test
    void aRevokedSessionCannotMoveMoney() throws Exception {
        jdbcTemplate.update("UPDATE user_sessions SET revoked_at = ? WHERE session_id = ?",
            Timestamp.from(now()), sessions.get("client:" + OWNER_ID));

        expect401(as(clientToken(OWNER_ID), json(post("/transactions/account/" + OWNER_ACCOUNT + "/withdraw"), FUNDS)),
            "session_revoked");

        assertUntouched(OWNER_ACCOUNT);
    }

    @Test
    void theProfileNeedsAToken() throws Exception {
        expect401(get("/user"), "unauthenticated");
        expect401(json(patch("/user"), "{\"name\":\"Changed\"}"), "unauthenticated");

        assertEquals("Security Test", jdbcTemplate.queryForObject("SELECT name FROM user_info WHERE user_id = ?", String.class, OWNER_ID));
    }

    @Test
    void theProfileOpensWithALiveSession() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/user")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(OWNER_ID + "@security.test"));
    }

    @Test
    void theProfileIsRefusedOnceTheSessionIsRevoked() throws Exception {
        jdbcTemplate.update("UPDATE user_sessions SET revoked_at = ? WHERE session_id = ?",
            Timestamp.from(now()), sessions.get("client:" + OWNER_ID));

        expect401(as(clientToken(OWNER_ID), get("/user")), "session_revoked");
        expect401(as(clientToken(OWNER_ID), json(patch("/user"), "{\"name\":\"Changed\"}")), "session_revoked");

        assertEquals("Security Test", jdbcTemplate.queryForObject("SELECT name FROM user_info WHERE user_id = ?", String.class, OWNER_ID));
    }

    @Test
    void anAdminCannotReadAClientsProfile() throws Exception {
        mockMvc.perform(as(adminToken(OWNER_ID), get("/user"))).andExpect(status().isForbidden());
    }

    @Test
    void aLoggedOutUserCanRequestAResetCode() throws Exception {
        mockMvc.perform(json(post("/user/resetpassword"), "{\"email\":\"nobody@security.test\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("User not found"));
    }

    @Test
    void aLoggedOutUserCanResetTheirPasswordAndEveryOldTokenStops() throws Exception {
        jdbcTemplate.update("UPDATE user_info SET code = '654321' WHERE user_id = ?", OWNER_ID);
        String oldToken = clientToken(OWNER_ID);
        mockMvc.perform(as(oldToken, get("/accounts/" + OWNER_ACCOUNT))).andExpect(status().isOk());

        mockMvc.perform(json(post("/user/resetpassword/reset"),
                "{\"email\":\"" + OWNER_ID + "@security.test\",\"code\":\"654321\",\"password\":\"NewSecure@@Pass\"}"))
            .andExpect(status().isOk());

        assertNotEquals("pass", jdbcTemplate.queryForObject("SELECT pass_hash FROM user_info WHERE user_id = ?", String.class, OWNER_ID));
        expect401(as(oldToken, get("/accounts/" + OWNER_ACCOUNT)), "session_revoked");
        mockMvc.perform(as(clientToken(OTHER_ID), get("/accounts/" + OTHER_ACCOUNT))).andExpect(status().isOk());
    }

    @Test
    void aResetWithAStaleTokenStillWorks() throws Exception {
        jdbcTemplate.update("UPDATE user_info SET code = '654321' WHERE user_id = ?", OWNER_ID);
        String expired = token(String.valueOf(OWNER_ID), "client", "access", Instant.now().minusSeconds(60), key);

        mockMvc.perform(as(expired, json(post("/user/resetpassword/reset"),
                "{\"email\":\"" + OWNER_ID + "@security.test\",\"code\":\"654321\",\"password\":\"NewSecure@@Pass\"}")))
            .andExpect(status().isOk());
    }

    private boolean isRevoked(UUID sid) {
        return jdbcTemplate.queryForObject("SELECT revoked_at FROM user_sessions WHERE session_id = ?", Timestamp.class, sid) != null;
    }

    @Test
    void anAdminLogsAClientOutEverywhere() throws Exception {
        UUID secondOwnerSession = insertSession("client", OWNER_ID);

        mockMvc.perform(as(adminToken(ADMIN_ID), post("/admin/users/" + OWNER_ID + "/logout"))).andExpect(status().isNoContent());

        assertTrue(isRevoked(sessions.get("client:" + OWNER_ID)));
        assertTrue(isRevoked(secondOwnerSession));
        assertEquals("admin_force_logout", jdbcTemplate.queryForObject(
            "SELECT revoked_reason FROM user_sessions WHERE session_id = ?", String.class, secondOwnerSession));
        assertFalse(isRevoked(sessions.get("client:" + OTHER_ID)));
        assertFalse(isRevoked(sessions.get("admin:" + OWNER_ID)));
        expect401(as(clientToken(OWNER_ID), get("/accounts/" + OWNER_ACCOUNT)), "session_revoked");
    }

    @Test
    void aClientCannotForceALogout() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), post("/admin/users/" + OTHER_ID + "/logout"))).andExpect(status().isForbidden());

        assertFalse(isRevoked(sessions.get("client:" + OTHER_ID)));
    }

    @Test
    void anAdminCanReadMoneyMovedAnalytics() throws Exception {
        mockMvc.perform(as(adminToken(ADMIN_ID), get("/admin/analytics/money-moved"))).andExpect(status().isOk());
    }

    @Test
    void aClientCannotReadMoneyMovedAnalytics() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/admin/analytics/money-moved"))).andExpect(status().isForbidden());
    }

    @Test
    void moneyMovedAnalyticsWithoutATokenIs401() throws Exception {
        expect401(get("/admin/analytics/money-moved"), "unauthenticated");
    }

    // The admin search routes: GET /admin/users, /admin/users/{id} and /admin/accounts/account/{accountId}
    private String[] adminSearchRoutes() {
        return new String[] {"/admin/users", "/admin/users/" + OTHER_ID, "/admin/accounts/account/" + OTHER_ACCOUNT};
    }

    @Test
    void anAdminCanUseTheSearchRoutes() throws Exception {
        for (String path : adminSearchRoutes()) {
            mockMvc.perform(as(adminToken(ADMIN_ID), get(path))).andExpect(status().isOk());
        }
    }

    @Test
    void aClientCannotUseTheSearchRoutes() throws Exception {
        for (String path : adminSearchRoutes()) {
            mockMvc.perform(as(clientToken(OWNER_ID), get(path))).andExpect(status().isForbidden());
        }
    }

    @Test
    void theSearchRoutesWithoutATokenAre401() throws Exception {
        for (String path : adminSearchRoutes()) {
            expect401(get(path), "unauthenticated");
        }
    }

    @Test
    void forceLogoutWithoutATokenIs401() throws Exception {
        expect401(post("/admin/users/" + OTHER_ID + "/logout"), "unauthenticated");

        assertFalse(isRevoked(sessions.get("client:" + OTHER_ID)));
    }

    @Test
    void aBadTokenOnAPublicRouteIsIgnored() throws Exception {
        jdbcTemplate.update("UPDATE user_sessions SET revoked_at = ? WHERE session_id = ?",
            Timestamp.from(now()), sessions.get("client:" + OWNER_ID));
        String expired = token(String.valueOf(OWNER_ID), "client", "access", Instant.now().minusSeconds(60), key);

        for (String bad : new String[] {clientToken(OWNER_ID), expired, "not-a-jwt"}) {
            mockMvc.perform(as(bad, get("/v3/api-docs"))).andExpect(status().isOk());
        }
    }

    // The old fallback secret was too short for HS256, so every token failed and everyone looked logged out,
    // with nothing in the log. Starting must fail loudly instead.
    @Test
    void startupRefusesAMissingOrTooShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtAuthenticationFilter("", null));
        assertThrows(IllegalStateException.class, () -> new JwtAuthenticationFilter("your-secret-key", null));
        assertDoesNotThrow(() -> new JwtAuthenticationFilter(SECRET, null));
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
        mockMvc.perform(as(adminToken(ADMIN_ID), get("/admin/accounts"))).andExpect(status().isOk());
    }

    @Test
    void aClientIsForbiddenFromAdminRoutes() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), get("/admin/accounts"))).andExpect(status().isForbidden());
    }

    @Test
    void anAdminCanReadAnyClientsAccount() throws Exception {
        mockMvc.perform(as(adminToken(ADMIN_ID), get("/accounts/" + OTHER_ACCOUNT))).andExpect(status().isOk());
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
        mockMvc.perform(as(adminToken(ADMIN_ID), patch("/accounts/close/" + OTHER_EMPTY_ACCOUNT))).andExpect(status().isOk());

        assertFalse(isActive(OTHER_EMPTY_ACCOUNT));
    }

    // Every other close rule still applies to admins
    @Test
    void anAdminCannotCloseAnAccountThatStillHasMoney() throws Exception {
        mockMvc.perform(as(adminToken(ADMIN_ID), patch("/accounts/close/" + OTHER_ACCOUNT))).andExpect(status().isBadRequest());

        assertTrue(isActive(OTHER_ACCOUNT));
    }

    // ---- Deposits and withdrawals: a client moves money in and out of their own account only ----

    private static final String FUNDS = "{\"amount\":100,\"fundingMethod\":\"bank\"}";

    private void assertUntouched(int accountId) {
        assertEquals(0, new BigDecimal("10000").compareTo(balance(accountId)), "balance of " + accountId + " moved: " + balance(accountId));
        assertEquals(0, count("SELECT count(*) FROM transactions WHERE account_id = ?", accountId),
            "a transaction row was written on " + accountId);
    }

    @Test
    void aClientCanDepositIntoTheirOwnAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/transactions/account/" + OWNER_ACCOUNT + "/deposit"), FUNDS)))
            .andExpect(status().isOk());

        assertEquals(0, new BigDecimal("10100").compareTo(balance(OWNER_ACCOUNT)));
    }

    @Test
    void aClientCanWithdrawFromTheirOwnAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/transactions/account/" + OWNER_ACCOUNT + "/withdraw"), FUNDS)))
            .andExpect(status().isOk());

        assertEquals(0, new BigDecimal("9900").compareTo(balance(OWNER_ACCOUNT)));
    }

    @Test
    void aClientCannotDepositIntoAnotherClientsAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/transactions/account/" + OTHER_ACCOUNT + "/deposit"), FUNDS)))
            .andExpect(status().isNotFound());

        assertUntouched(OTHER_ACCOUNT);
    }

    @Test
    void aClientCannotWithdrawFromAnotherClientsAccount() throws Exception {
        mockMvc.perform(as(clientToken(OWNER_ID), json(post("/transactions/account/" + OTHER_ACCOUNT + "/withdraw"), FUNDS)))
            .andExpect(status().isNotFound());

        assertUntouched(OTHER_ACCOUNT);
    }

    // Admin 920101 shares its sub with client 920101, the owner of OWNER_ACCOUNT
    @Test
    void anAdminCannotDepositOrWithdrawEvenWithTheOwnersId() throws Exception {
        for (String action : new String[] {"deposit", "withdraw"}) {
            mockMvc.perform(as(adminToken(OWNER_ID), json(post("/transactions/account/" + OWNER_ACCOUNT + "/" + action), FUNDS)))
                .andExpect(status().isForbidden());
        }

        assertUntouched(OWNER_ACCOUNT);
    }

    // The controller refuses non-clients too, so a plain 403 can't show the security rule exists. An invalid body can:
    // the rule refuses before the body is read, while the controller would answer the failed validation with 400
    @Test
    void theSecurityRuleRefusesAnAdminBeforeTheBodyIsValidated() throws Exception {
        for (String action : new String[] {"deposit", "withdraw"}) {
            mockMvc.perform(as(adminToken(ADMIN_ID), json(post("/transactions/account/" + OWNER_ACCOUNT + "/" + action), "{\"amount\":0}")))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    void depositAndWithdrawWithoutATokenGet401() throws Exception {
        for (String action : new String[] {"deposit", "withdraw"}) {
            mockMvc.perform(json(post("/transactions/account/" + OWNER_ACCOUNT + "/" + action), FUNDS))
                .andExpect(status().isUnauthorized());
        }

        assertUntouched(OWNER_ACCOUNT);
    }
}
