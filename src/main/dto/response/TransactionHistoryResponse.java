package dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransactionHistoryResponse {

    @Schema(description = "Unique identifier of the transaction")
    @JsonProperty("transaction_id")
    private Integer transactionId;

    @Schema(description = "Type of the transaction: WITHDRAWAL, DEPOSIT or CURRENCY EXCHANGE. Trades are not recorded here; see the account's orders")
    @JsonProperty("transaction_type")
    private String transactionType;

    @Schema(description = "Amount involved in the transaction")
    @JsonProperty("amount")
    private BigDecimal amount;

    @Schema(description = "Direction of the money: IN, OUT or EXCHANGE")
    @JsonProperty("side")
    private String side;

    @Schema(description = "Account associated with the transaction")
    @JsonProperty("account_id")
    private Integer accountId;

    @Schema(description = "Timestamp when the transaction occurred")
    @JsonProperty("happened_at")
    private LocalDateTime happenedAt;
}
