package dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class AdminLoginResponse {
    @Schema(description = "admin.admin_id of the admin who logged in")
    private Integer id;

    @Schema(description = "Token role mapped from admin.role", allowableValues = {"superadmin", "admin", "analyst"})

    private String role;

    @Schema(description = "The new session's id; the auth service puts it in every token as the sid claim", format = "uuid")
    private String sid;

    @Schema(description = "When the session ends however active it is (ISO-8601, UTC)", example = "2026-10-15T17:00:00Z")
    private String expiresAt;

    public AdminLoginResponse(Integer id, String role, String sid, String expiresAt) {
        this.id = id;
        this.role = role;
        this.sid = sid;
        this.expiresAt = expiresAt;
    }

    public Integer getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getSid() {
        return sid;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
