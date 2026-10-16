package exception;

public class SessionRejectedException extends RuntimeException {
    public static final String INVALID = "session_invalid";
    public static final String REVOKED = "session_revoked";
    public static final String EXPIRED = "session_expired";
    public static final String IDLE = "session_idle";

    private final String code;

    public SessionRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static SessionRejectedException invalid() {
        return new SessionRejectedException(INVALID, "Session not found");
    }

    public static SessionRejectedException revoked() {
        return new SessionRejectedException(REVOKED, "Session has been ended, please log in again");
    }

    public static SessionRejectedException expired() {
        return new SessionRejectedException(EXPIRED, "Session has reached its maximum length, please log in again");
    }

    public static SessionRejectedException idle() {
        return new SessionRejectedException(IDLE, "Session ended after a period of inactivity, please log in again");
    }

    public String getCode() {
        return code;
    }
}
