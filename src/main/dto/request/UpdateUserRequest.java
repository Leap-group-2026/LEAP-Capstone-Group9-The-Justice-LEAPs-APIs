package dto.request;

public record UpdateUserRequest(
    String name,
    String email,
    String address
) {}
