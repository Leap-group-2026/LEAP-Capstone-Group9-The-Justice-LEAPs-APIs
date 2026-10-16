package dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record RevokeSessionRequest(
    @Schema(description = "Why the session ends, stored as user_sessions.revoked_reason. Defaults to \"logout\".",
        example = "refresh_token_reuse", nullable = true)
    String reason
) {}
