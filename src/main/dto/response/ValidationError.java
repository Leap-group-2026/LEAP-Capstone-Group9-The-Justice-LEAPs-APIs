package main.dto.response;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

public record ValidationError(
    int status,
    String message,
    LocalDateTime timestamp,
    @Schema(description = "What failed: the request field, the order rule (balance, price, instrument, holdings, market_hours), or the resource type for a 404")
    String fieldName,
    @Schema(description = "The rejected value for validation errors, or the id for a 404; null for order-rule failures", nullable = true)
    Object rejectedValue,
    @Schema(description = "Human-readable reason")
    String fieldMessage
) {}
