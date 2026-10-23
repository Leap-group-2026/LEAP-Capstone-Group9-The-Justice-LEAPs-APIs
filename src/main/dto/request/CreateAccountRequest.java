package dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import entities.PortfolioSize;
import java.math.BigDecimal;

public record CreateAccountRequest(
    @Schema(description = "Opening balance", example = "1000.00")
    @NotNull
    @PositiveOrZero
    BigDecimal balance,

    @Schema(example = "BALANCED")
    @NotNull
    PortfolioSize portfolioSize,

    @Schema(example = "Passive")
    @NotBlank
    String tradeType,

    @Schema(description = "Optional name, trimmed, at most 40 characters, no control characters. Omitted, null or blank means no name",
        example = "Savings", nullable = true)
    @AccountName
    String accountName
) {}
