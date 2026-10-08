package dto.response;

public record UpdateUserResponse(
    Integer userId,
    String name,
    String email,
    String address
) {}
