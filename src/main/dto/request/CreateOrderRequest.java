package main.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
    @NotNull
    String side,
    
    @NotNull
    Integer accountId,
    
    @NotNull
    Integer instrumentId,
    
    @NotNull
    @Positive
    Integer quantity
) {}
