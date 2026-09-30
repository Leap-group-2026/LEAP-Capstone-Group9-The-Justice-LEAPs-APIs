package main.exception;

public class TransactionProcessingException extends RuntimeException {
    private final Integer orderId;
    private final String reason;

    public TransactionProcessingException(Integer orderId, String reason, Throwable cause) {
        super(String.format("Order %d transaction failed: %s", orderId, reason), cause);
        this.orderId = orderId;
        this.reason = reason;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }
}
