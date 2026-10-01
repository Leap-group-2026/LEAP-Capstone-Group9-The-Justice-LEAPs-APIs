package main.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionRequest(
    @NotNull 
    String baseAndExchange,
    
    @NotNull
    BigDecimal amount, 

    @NotNull
    String side,
    
    @NotNull
    Integer accountId,
    
    @NotNull
    String transactionType
) {}
