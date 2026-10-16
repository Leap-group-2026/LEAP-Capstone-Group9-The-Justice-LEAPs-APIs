package services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import entities.SessionEntity;
import exception.SessionRejectedException;
import repos.SessionRepo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SessionService {
    public static final String CLIENT_ROLE = "client";

    static final Duration TOUCH_INTERVAL = Duration.ofSeconds(60);

    // UUID.fromString also accepts non-canonical forms such as "1-1-1-1-1"
    private static final Pattern UUID_FORMAT =
        Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final SessionRepo repo;
    private final Clock clock;
    private final Duration idleTimeout;
    private final Duration maxLifetime;

    public record Grant(String sid, Instant expiresAt) {}

    public SessionService(SessionRepo repo, Clock clock,
            @Value("${session.idle-timeout:${SESSION_IDLE_TIMEOUT:PT30M}}") String idleTimeout,
            @Value("${session.max-lifetime:${SESSION_MAX_LIFETIME:PT8H}}") String maxLifetime) {
        this.repo = repo;
        this.clock = clock;
        this.idleTimeout = Duration.parse(idleTimeout);
        this.maxLifetime = Duration.parse(maxLifetime);
        if (this.idleTimeout.isNegative() || this.idleTimeout.isZero()
                || this.maxLifetime.isNegative() || this.maxLifetime.isZero()) {
            throw new IllegalStateException("session.idle-timeout and session.max-lifetime must be positive ISO-8601 durations");
        }
    }

    public Grant create(String role, Integer id) {
        if (role == null || id == null) {
            throw new IllegalArgumentException("A session needs a role and an owner id");
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);

        SessionEntity session = new SessionEntity();
        session.setSessionId(UUID.randomUUID());
        if (CLIENT_ROLE.equals(role)) {
            session.setUserId(id);
        } else {
            session.setAdminId(id);
        }
        session.setRole(role);
        session.setCreatedAt(now);
        session.setLastActivityAt(now);
        session.setExpiresAt(now.plus(maxLifetime));
        repo.insert(session);

        return new Grant(session.getSessionId().toString(), session.getExpiresAt());
    }

    public SessionEntity checkAndTouch(String sid, String role, String sub) {
        SessionEntity session = find(sid);
        if (!ownedBy(session, role, sub)) {
            throw SessionRejectedException.invalid();
        }
        requireLive(session);
        touch(session);
        return session;
    }

    public SessionEntity checkAndTouch(String sid) {
        SessionEntity session = find(sid);
        requireLive(session);
        touch(session);
        return session;
    }

    public void revoke(String sid, String reason) {
        UUID id = parse(sid);
        if (id != null) {
            repo.revoke(id, clock.instant(), reason);
        }
    }

    public int revokeAllForUser(Integer userId, String reason) {
        return repo.revokeAllForUser(userId, clock.instant(), reason);
    }

    public int revokeAllForAdmin(Integer adminId, String reason) {
        return repo.revokeAllForAdmin(adminId, clock.instant(), reason);
    }

    private SessionEntity find(String sid) {
        UUID id = parse(sid);
        if (id == null) {
            throw SessionRejectedException.invalid();
        }
        SessionEntity session = repo.findById(id).orElseThrow(SessionRejectedException::invalid);
        session.setSessionId(id);
        return session;
    }

    private static boolean ownedBy(SessionEntity session, String role, String sub) {
        Integer owner;
        try {
            owner = Integer.valueOf(sub);
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
        boolean clientToken = CLIENT_ROLE.equals(role);
        boolean clientSession = CLIENT_ROLE.equals(session.getRole());
        Integer sessionOwner = clientToken ? session.getUserId() : session.getAdminId();
        return role != null && clientToken == clientSession && owner.equals(sessionOwner);
    }

    private void requireLive(SessionEntity session) {
        if (session.getRevokedAt() != null) {
            throw SessionRejectedException.revoked();
        }
        Instant now = clock.instant();
        if (!now.isBefore(session.getExpiresAt())) {
            throw SessionRejectedException.expired();
        }
        if (!now.isBefore(session.getLastActivityAt().plus(idleTimeout))) {
            throw SessionRejectedException.idle();
        }
    }

    private void touch(SessionEntity session) {
        Instant now = clock.instant();
        Instant staleBefore = now.minus(TOUCH_INTERVAL);
        if (session.getLastActivityAt().isBefore(staleBefore)) {
            repo.touch(session.getSessionId(), now, staleBefore);
            session.setLastActivityAt(now);
        }
    }

    private static UUID parse(String sid) {
        return sid != null && UUID_FORMAT.matcher(sid).matches() ? UUID.fromString(sid) : null;
    }
}
