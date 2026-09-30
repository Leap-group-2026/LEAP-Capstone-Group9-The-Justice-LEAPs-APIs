package main.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record OrderSubmissionResponse(
    @JsonProperty("order_id")
    Integer orderId,
    
    @JsonProperty("created_at")
    LocalDateTime createdAt
) {}
