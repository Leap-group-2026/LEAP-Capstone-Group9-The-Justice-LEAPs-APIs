package dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

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
