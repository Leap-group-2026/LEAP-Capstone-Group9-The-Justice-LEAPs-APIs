package main.exception;

public class InvalidOrderException extends RuntimeException {
    private final String field;
    private final String reason;

    /**
     * Constructs an InvalidOrderException with field and reason
     * 
     * @param field  The order field that failed validation (e.g., "side", "quantity")
     * @param reason The reason for the validation failure
     */
    public InvalidOrderException(String field, String reason) {
        super(String.format("Invalid order %s: %s", field, reason));
        this.field = field;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }
}
