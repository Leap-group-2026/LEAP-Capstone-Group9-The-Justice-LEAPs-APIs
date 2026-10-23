import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import exception.SessionRejectedException;
import main.Application;
import repos.SessionRepo;
import services.SessionService;

@SpringBootTest(classes = Application.class)
@Transactional
public class SessionServiceTest {

    private static final int CLIENT_ID = 930201;
    private static final int OTHER_CLIENT_ID = 930202;
    private static final int ADMIN_ID = 930201;
    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");

    @Autowired
    private SessionRepo repo;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MovableClock clock;
    private SessionService sessions;

    static class MovableClock extends Clock {
        private Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @BeforeEach
    void setUp() {
        for (int userId : new int[] {CLIENT_ID, OTHER_CLIENT_ID}) {
            jdbcTemplate.update(
                "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
                "VALUES (?, 'Session Test', ?, DATE '1990-01-01', '1 Test St', ?, 'pass')",
                userId, userId + "@session.test", "ssn-" + userId);
        }
        jdbcTemplate.update("INSERT INTO admin (admin_id, email, pass_hash, role) VALUES (?, 'session-admin', 'pass', 'ADMIN')", ADMIN_ID);
        clock = new MovableClock(START);
        sessions = new SessionService(repo, clock, "PT30M", "PT8H");
    }

    private Instant column(String column, String sid) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM user_sessions WHERE session_id = ?",
            Timestamp.class, UUID.fromString(sid)).toInstant();
    }

    private void assertRejected(String code, Executable call) {
        SessionRejectedException e = assertThrows(SessionRejectedException.class, call);
        assertEquals(code, e.getCode());
    }

    @Test
    void aClientSessionIsOwnedByUserIdAndCappedAtEightHours() {
        SessionService.Grant grant = sessions.create("client", CLIENT_ID);

        assertEquals(START.plus(Duration.ofHours(8)), grant.expiresAt());
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM user_sessions WHERE session_id = ? AND user_id = ? AND admin_id IS NULL AND role = 'client'",
            Integer.class, UUID.fromString(grant.sid()), CLIENT_ID));
        assertEquals(START, column("created_at", grant.sid()));
        assertEquals(START, column("last_activity_at", grant.sid()));
        assertEquals(grant.expiresAt(), column("expires_at", grant.sid()));
    }

    @Test
    void anAdminSessionIsOwnedByAdminIdAndKeepsItsRole() {
        SessionService.Grant grant = sessions.create("analyst", ADMIN_ID);

        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM user_sessions WHERE session_id = ? AND admin_id = ? AND user_id IS NULL AND role = 'analyst'",
            Integer.class, UUID.fromString(grant.sid()), ADMIN_ID));
    }

    @Test
    void everyLoginGetsItsOwnSession() {
        assertNotEquals(sessions.create("client", CLIENT_ID).sid(), sessions.create("client", CLIENT_ID).sid());
    }

    @Test
    void aLiveSessionPassesForItsOwner() {
        String sid = sessions.create("client", CLIENT_ID).sid();
        clock.advance(Duration.ofMinutes(29));

        assertDoesNotThrow(() -> sessions.checkAndTouch(sid, "client", String.valueOf(CLIENT_ID)));
    }

    @Test
    void activityIsRecordedOnlyWhenTheLastRecordIsOlderThanSixtySeconds() {
        String sid = sessions.create("client", CLIENT_ID).sid();

        clock.advance(Duration.ofSeconds(59));
        sessions.checkAndTouch(sid, "client", String.valueOf(CLIENT_ID));
        assertEquals(START, column("last_activity_at", sid), "a request within 60 s wrote to the row");

        clock.advance(Duration.ofSeconds(2));
        sessions.checkAndTouch(sid, "client", String.valueOf(CLIENT_ID));
        assertEquals(START.plusSeconds(61), column("last_activity_at", sid));
    }

    @Test
    void regularActivityKeepsASessionAlivePastThirtyMinutes() {
        String sid = sessions.create("client", CLIENT_ID).sid();
        for (int i = 0; i < 4; i++) {
            clock.advance(Duration.ofMinutes(20));
            sessions.checkAndTouch(sid, "client", String.valueOf(CLIENT_ID));
        }
        assertEquals(START.plus(Duration.ofMinutes(80)), column("last_activity_at", sid));
    }

    @Test
    void thirtyMinutesWithoutActivityIsIdle() {
        String sid = sessions.create("client", CLIENT_ID).sid();
        clock.advance(Duration.ofMinutes(30));

        assertRejected("session_idle", () -> sessions.checkAndTouch(sid, "client", String.valueOf(CLIENT_ID)));
    }

    @Test
    void theAbsoluteCapEndsAnActiveSession() {
        String sid = sessions.create("client", CLIENT_ID).sid();
        for (int i = 0; i < 23; i++) {
            clock.advance(Duration.ofMinutes(20));
            sessions.checkAndTouch(sid, "client", String.valueOf(CLIENT_ID));
        }
        clock.advance(Duration.ofMinutes(20));
        assertRejected("session_expired", () -> sessions.checkAndTouch(sid, "client", String.valueOf(CLIENT_ID)));
    }

    @Test
    void aRevokedSessionIsRevokedEvenBeforeItsCap() {
        String sid = sessions.create("client", CLIENT_ID).sid();
        sessions.revoke(sid, "logout");

        assertRejected("session_revoked", () -> sessions.checkAndTouch(sid, "client", String.valueOf(CLIENT_ID)));
    }

    @Test
    void unknownAndMalformedSidsAreInvalid() {
        assertRejected("session_invalid", () -> sessions.checkAndTouch(UUID.randomUUID().toString(), "client", String.valueOf(CLIENT_ID)));
        assertRejected("session_invalid", () -> sessions.checkAndTouch("security-test-session", "client", String.valueOf(CLIENT_ID)));
        assertRejected("session_invalid", () -> sessions.checkAndTouch("1-1-1-1-1", "client", String.valueOf(CLIENT_ID)));
        assertRejected("session_invalid", () -> sessions.checkAndTouch(null, "client", String.valueOf(CLIENT_ID)));
    }

    @Test
    void anotherClientsSidIsInvalid() {
        String sid = sessions.create("client", CLIENT_ID).sid();

        assertRejected("session_invalid", () -> sessions.checkAndTouch(sid, "client", String.valueOf(OTHER_CLIENT_ID)));
    }

    @Test
    void anAdminTokenCannotUseAClientsSidWithTheSameNumber() {
        String clientSid = sessions.create("client", CLIENT_ID).sid();
        String adminSid = sessions.create("admin", ADMIN_ID).sid();

        assertRejected("session_invalid", () -> sessions.checkAndTouch(clientSid, "admin", String.valueOf(ADMIN_ID)));
        assertRejected("session_invalid", () -> sessions.checkAndTouch(adminSid, "client", String.valueOf(CLIENT_ID)));
        assertDoesNotThrow(() -> sessions.checkAndTouch(adminSid, "admin", String.valueOf(ADMIN_ID)));
    }

    @Test
    void aNonNumericOrMissingSubIsInvalid() {
        String sid = sessions.create("client", CLIENT_ID).sid();

        assertRejected("session_invalid", () -> sessions.checkAndTouch(sid, "client", "abc"));
        assertRejected("session_invalid", () -> sessions.checkAndTouch(sid, "client", null));
        assertRejected("session_invalid", () -> sessions.checkAndTouch(sid, null, String.valueOf(CLIENT_ID)));
    }

    @Test
    void theSidOnlyCheckAppliesTheSameRules() {
        String sid = sessions.create("client", CLIENT_ID).sid();
        clock.advance(Duration.ofMinutes(2));

        sessions.checkAndTouch(sid);
        assertEquals(START.plus(Duration.ofMinutes(2)), column("last_activity_at", sid));

        clock.advance(Duration.ofMinutes(31));
        assertRejected("session_idle", () -> sessions.checkAndTouch(sid));
    }

    @Test
    void revokingTwiceKeepsTheFirstTimeAndReason() {
        String sid = sessions.create("client", CLIENT_ID).sid();
        sessions.revoke(sid, "logout");
        clock.advance(Duration.ofMinutes(5));
        sessions.revoke(sid, "refresh_token_reuse");

        assertEquals(START, column("revoked_at", sid));
        assertEquals("logout", jdbcTemplate.queryForObject(
            "SELECT revoked_reason FROM user_sessions WHERE session_id = ?", String.class, UUID.fromString(sid)));
    }

    @Test
    void revokingAnUnknownOrMalformedSidDoesNothing() {
        assertDoesNotThrow(() -> sessions.revoke(UUID.randomUUID().toString(), "logout"));
        assertDoesNotThrow(() -> sessions.revoke("not-a-uuid", "logout"));
    }

    @Test
    void revokeAllForUserEndsEveryOneOfTheirSessionsAndNobodyElses() {
        String first = sessions.create("client", CLIENT_ID).sid();
        String second = sessions.create("client", CLIENT_ID).sid();
        String otherClient = sessions.create("client", OTHER_CLIENT_ID).sid();
        String adminSameNumber = sessions.create("admin", ADMIN_ID).sid();

        assertEquals(2, sessions.revokeAllForUser(CLIENT_ID, "password_reset"));

        assertRejected("session_revoked", () -> sessions.checkAndTouch(first));
        assertRejected("session_revoked", () -> sessions.checkAndTouch(second));
        assertDoesNotThrow(() -> sessions.checkAndTouch(otherClient));
        assertDoesNotThrow(() -> sessions.checkAndTouch(adminSameNumber));
    }

    @Test
    void revokeAllForAdminLeavesTheClientWithTheSameNumberAlone() {
        String adminSid = sessions.create("admin", ADMIN_ID).sid();
        String clientSid = sessions.create("client", CLIENT_ID).sid();

        assertEquals(1, sessions.revokeAllForAdmin(ADMIN_ID, "admin_force_logout"));

        assertRejected("session_revoked", () -> sessions.checkAndTouch(adminSid));
        assertDoesNotThrow(() -> sessions.checkAndTouch(clientSid));
    }

    @Test
    void deletingTheUserRemovesTheirSessions() {
        String sid = sessions.create("client", CLIENT_ID).sid();
        jdbcTemplate.update("DELETE FROM user_info WHERE user_id = ?", CLIENT_ID);

        assertRejected("session_invalid", () -> sessions.checkAndTouch(sid));
    }
}
