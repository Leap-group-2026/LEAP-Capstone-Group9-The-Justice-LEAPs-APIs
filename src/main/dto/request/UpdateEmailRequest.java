package main.dto.request;

public record UpdateEmailRequest(
    Integer userId,
    String email
) {}
