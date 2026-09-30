package main.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransactionHistoryResponse {

    @Schema(description = "Unique identifier of the transaction")
    @JsonProperty("transaction_id")
    private Integer transactionId;

    @Schema(description = "Type of the transaction")
    @JsonProperty("transaction_type")
    private String transactionType;

    @Schema(description = "Amount involved in the transaction")
    @JsonProperty("amount")
    private BigDecimal amount;

    @Schema(description = "Side of the transaction (e.g., IN or OUT)")
    @JsonProperty("side")
    private String side;

    @Schema(description = "Account associated with the transaction")
    @JsonProperty("account_id")
    private Integer accountId;

    @Schema(description = "Timestamp when the transaction occurred")
    @JsonProperty("happened_at")
    private LocalDateTime happenedAt;
}