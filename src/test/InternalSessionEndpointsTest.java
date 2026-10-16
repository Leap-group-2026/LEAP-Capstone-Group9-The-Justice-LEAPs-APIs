import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import config.InternalApiKeyFilter;
import main.Application;
import test.config.TestClockConfig;

@SpringBootTest(classes = Application.class)
@Import(TestClockConfig.class)
@AutoConfigureMockMvc
@Transactional
public class InternalSessionEndpointsTest {

    private static final int CLIENT_ID = 930401;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private Clock clock;
    @Value("${internal.api.key}")
    private String internalApiKey;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Internal Test', 'internal@example.com', DATE '1990-01-01', '1 Test St', 'ssn-930401', 'pass')", CLIENT_ID);
    }

    private Instant now() {
        return clock.instant();
    }

    private UUID session(Instant lastActivity, Instant expiresAt) {
        UUID sid = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO user_sessions (session_id, user_id, role, created_at, last_activity_at, expires_at) VALUES (?, ?, 'client', ?, ?, ?)",
            sid, CLIENT_ID, Timestamp.from(now().minus(Duration.ofHours(1))), Timestamp.from(lastActivity), Timestamp.from(expiresAt));
        return sid;
    }

    private UUID liveSession() {
        return session(now().minus(Duration.ofMinutes(5)), now().plus(Duration.ofHours(2)));
    }

    private Instant lastActivity(UUID sid) {
        return jdbcTemplate.queryForObject("SELECT last_activity_at FROM user_sessions WHERE session_id = ?", Timestamp.class, sid).toInstant();
    }

    private String revokedReason(UUID sid) {
        return jdbcTemplate.queryForObject("SELECT revoked_reason FROM user_sessions WHERE session_id = ?", String.class, sid);
    }

    private Timestamp revokedAt(UUID sid) {
        return jdbcTemplate.queryForObject("SELECT revoked_at FROM user_sessions WHERE session_id = ?", Timestamp.class, sid);
    }

    private MockHttpServletRequestBuilder withKey(MockHttpServletRequestBuilder request) {
        return request.header(InternalApiKeyFilter.HEADER, internalApiKey);
    }

    @Test
    void withoutTheKeyBothEndpointsAre401AndNothingChanges() throws Exception {
        UUID sid = liveSession();
        Instant before = lastActivity(sid);

        mockMvc.perform(post("/internal/sessions/" + sid + "/check"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString(InternalApiKeyFilter.HEADER)));
        mockMvc.perform(post("/internal/sessions/" + sid + "/revoke"))
            .andExpect(status().isUnauthorized());

        assertEquals(before, lastActivity(sid), "a request without the key touched the session");
        assertNull(revokedAt(sid), "a request without the key revoked the session");
    }

    @Test
    void aWrongKeyIs401AndNothingChanges() throws Exception {
        UUID sid = liveSession();

        mockMvc.perform(post("/internal/sessions/" + sid + "/revoke").header(InternalApiKeyFilter.HEADER, internalApiKey + "x"))
            .andExpect(status().isUnauthorized());

        assertNull(revokedAt(sid));
    }

    @Test
    void anyInternalPathNeedsTheKey() throws Exception {
        mockMvc.perform(post("/internal/anything")).andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString(InternalApiKeyFilter.HEADER)));
    }

    @Test
    void checkingALiveSessionAnswersItsCapAndCountsAsActivity() throws Exception {
        Instant cap = now().plus(Duration.ofHours(2));
        UUID sid = session(now().minus(Duration.ofMinutes(5)), cap);

        mockMvc.perform(withKey(post("/internal/sessions/" + sid + "/check")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expiresAt").value(cap.toString()));

        assertEquals(now(), lastActivity(sid));
    }

    @Test
    void checkingADeadSessionAnswers401WithTheContractCode() throws Exception {
        UUID revoked = liveSession();
        jdbcTemplate.update("UPDATE user_sessions SET revoked_at = ? WHERE session_id = ?", Timestamp.from(now()), revoked);
        UUID expired = session(now(), now());
        UUID idle = session(now().minus(Duration.ofMinutes(31)), now().plus(Duration.ofHours(2)));

        expectCheck401(revoked.toString(), "session_revoked");
        expectCheck401(expired.toString(), "session_expired");
        expectCheck401(idle.toString(), "session_idle");
        expectCheck401(UUID.randomUUID().toString(), "session_invalid");
        expectCheck401("not-a-uuid", "session_invalid");
    }

    private void expectCheck401(String sid, String code) throws Exception {
        mockMvc.perform(withKey(post("/internal/sessions/" + sid + "/check")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value(code))
            .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void revokeEndsTheSessionWithTheGivenReason() throws Exception {
        UUID sid = liveSession();

        mockMvc.perform(withKey(post("/internal/sessions/" + sid + "/revoke"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"refresh_token_reuse\"}"))
            .andExpect(status().isNoContent());

        assertNotNull(revokedAt(sid));
        assertEquals("refresh_token_reuse", revokedReason(sid));
        expectCheck401(sid.toString(), "session_revoked");
    }

    @Test
    void revokeWithoutABodyRecordsLogout() throws Exception {
        UUID sid = liveSession();

        mockMvc.perform(withKey(post("/internal/sessions/" + sid + "/revoke"))).andExpect(status().isNoContent());

        assertEquals("logout", revokedReason(sid));
    }

    @Test
    void revokeIsIdempotentAndKeepsTheFirstReason() throws Exception {
        UUID sid = liveSession();

        mockMvc.perform(withKey(post("/internal/sessions/" + sid + "/revoke"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"logout\"}")).andExpect(status().isNoContent());
        Timestamp first = revokedAt(sid);
        mockMvc.perform(withKey(post("/internal/sessions/" + sid + "/revoke"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"refresh_token_reuse\"}")).andExpect(status().isNoContent());

        assertEquals(first, revokedAt(sid));
        assertEquals("logout", revokedReason(sid));
    }

    @Test
    void revokingAnUnknownOrMalformedSidIs204() throws Exception {
        mockMvc.perform(withKey(post("/internal/sessions/" + UUID.randomUUID() + "/revoke"))).andExpect(status().isNoContent());
        mockMvc.perform(withKey(post("/internal/sessions/not-a-uuid/revoke"))).andExpect(status().isNoContent());
    }
}
