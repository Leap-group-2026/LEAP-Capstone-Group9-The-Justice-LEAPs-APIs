package dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class AdminLoginResponse {
    @Schema(description = "admin.admin_id of the admin who logged in")
    private Integer id;

    @Schema(description = "Token role mapped from admin.role", allowableValues = {"superadmin", "admin", "analyst"})

    private String role;

    public AdminLoginResponse(Integer id, String role) {
        this.id = id;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public String getRole() {
        return role;
    }
}