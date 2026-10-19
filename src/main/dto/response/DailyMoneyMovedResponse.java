package dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;


public record DailyMoneyMovedResponse(
    @Schema(description = "Fill date (Eastern time)", example = "2024-09-24") LocalDate date,
    @Schema(description = "Sum of total_price of filled BUY orders that day", example = "31400.0000") BigDecimal buyTotal,
    @Schema(description = "Sum of total_price of filled SELL orders that day", example = "16810.0000") BigDecimal sellTotal,
    @Schema(description = "Number of orders filled that day", example = "12") Long orders
) {
}
