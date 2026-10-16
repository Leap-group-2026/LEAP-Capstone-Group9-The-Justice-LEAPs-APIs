package dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class SessionCheckResponse {
    @Schema(description = "When the session ends however active it is (ISO-8601, UTC)", example = "2026-10-15T17:00:00Z")
    private String expiresAt;

    public SessionCheckResponse(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
