package dto.request;

import java.math.BigDecimal;
import java.util.Locale;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record FundsTransferRequest(
    @Schema(description = "Dollars to move, greater than 0, at most 100000 and at most 2 decimal places", example = "250.00")
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    @DecimalMax(value = "100000", message = "amount must be at most 100000")
    @Digits(integer = 6, fraction = 2, message = "amount must have at most 2 decimal places")
    BigDecimal amount,

    @Schema(description = "Optional label for where the money is simulated to come from or go to; any casing, not stored",
        allowableValues = {"bank", "debit"}, nullable = true)
    @Pattern(regexp = "bank|debit", message = "fundingMethod must be bank or debit")
    String fundingMethod
) {
    public FundsTransferRequest {
        if (fundingMethod != null) {
            fundingMethod = fundingMethod.toLowerCase(Locale.ROOT);
        }
    }
}
