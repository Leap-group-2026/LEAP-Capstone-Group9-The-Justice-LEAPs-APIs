package dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record RenameAccountRequest(
    @Schema(description = "New name, trimmed, at most 40 characters, no control characters. null or blank removes the name",
        example = "Savings", nullable = true)
    @AccountName
    String name
) {}
