package dto.response;

import java.math.BigDecimal;

public record OrderStatusAnalyticsResponse (
    String status,
    Long orderCount,
    BigDecimal percentage
) {
}
