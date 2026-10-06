package dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class UserLoginResponse {
    @Schema(description = "user_info.user_id of the client who logged in")
    private Integer id;

    public UserLoginResponse(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}