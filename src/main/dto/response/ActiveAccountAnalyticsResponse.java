package dto.response;

import java.math.BigDecimal;

public record ActiveAccountAnalyticsResponse(
    Integer accountId,
    Long orderCount,
    Long filledQuantity,
    BigDecimal totalTradedValue,
    Long buyCount,
    Long sellCount
) {
}
