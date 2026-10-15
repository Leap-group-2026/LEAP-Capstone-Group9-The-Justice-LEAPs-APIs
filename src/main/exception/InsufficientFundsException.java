package exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    private final BigDecimal requested;
    private final BigDecimal balance;

    public InsufficientFundsException(BigDecimal requested, BigDecimal balance) {
        super(String.format("Insufficient balance for withdrawal: balance=$%s, requested=$%s", balance, requested));
        this.requested = requested;
        this.balance = balance;
    }

    public BigDecimal getRequested() {
        return requested;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
