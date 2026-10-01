package main.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSubmittedEvent(

    UUID eventId,
    Integer orderId,
    Integer accountId,
    Integer instrumentId,
    String side,
    Integer quantity,
    BigDecimal estimatedTotal,
    LocalDateTime submittedAt,
    int version
) {}