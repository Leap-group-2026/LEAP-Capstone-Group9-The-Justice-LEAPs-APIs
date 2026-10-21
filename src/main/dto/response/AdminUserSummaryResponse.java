package dto.response;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

// What GET /admin/users shows of each user; deliberately has no pass_hash, ssn_hash or reset code
public record AdminUserSummaryResponse(
    @Schema(example = "42") Integer userId,
    @Schema(example = "Jane Smith") String name,
    @Schema(example = "jane@example.com") String email,
    @Schema(example = "1990-01-01") LocalDate dateOfBirth,
    @Schema(example = "1 Main St") String address
) {
}
