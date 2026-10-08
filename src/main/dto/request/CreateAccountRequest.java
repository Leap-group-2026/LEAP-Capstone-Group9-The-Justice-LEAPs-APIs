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
    String tradeType
) {}
