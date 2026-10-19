package dto.response;

import java.time.LocalDate;
import java.util.List;

public record AdminUserLookupResponse(
    Integer userId,
    String name,
    String email,
    LocalDate dateOfBirth,
    String address,
    List<AccountResponse> accounts
) {}