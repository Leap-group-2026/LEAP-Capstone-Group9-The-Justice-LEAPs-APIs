package dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CloseAccountRequest(
    @Schema(description = "Id of the user closing the account; must be the account's owner", example = "1")
    @NotNull
    Integer userId
) {}
