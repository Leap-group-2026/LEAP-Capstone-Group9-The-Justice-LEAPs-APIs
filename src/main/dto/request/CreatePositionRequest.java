package dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePositionRequest(
    @Schema(example = "1")
    @NotNull
    Integer accountId,

    @Schema(example = "1")
    @NotNull
    Integer instrumentId,

    @Schema(example = "10")
    @NotNull
    @Positive
    Integer quantity,

    @Schema(description = "Total cost of the position", example = "1895.00")
    @NotNull
    BigDecimal totalPrice,

    @Schema(description = "Average cost per unit", example = "189.50")
    @NotNull
    BigDecimal averagePrice,

    @Schema(description = "Defaults to now when omitted", nullable = true)
    LocalDateTime openedAt,

    @Schema(description = "Omit for an open position", nullable = true)
    LocalDateTime closedAt
) {}
