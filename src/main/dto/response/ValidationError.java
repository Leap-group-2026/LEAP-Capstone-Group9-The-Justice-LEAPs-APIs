package main.dto.response;

import java.time.LocalDateTime;

public record ValidationError(
    int status,
    String message,
    LocalDateTime timestamp,
    String fieldName,
    Object rejectedValue,
    String fieldMessage
) {}
