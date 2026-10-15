package dto.response;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

public record FundsTransferResponse(
    @Schema(description = "The recorded transaction, in the same shape GET /transactions/account/{accountId} returns")
    TransactionHistoryResponse transaction,
    @Schema(description = "The account's balance after the transaction")
    BigDecimal balance
) {}
