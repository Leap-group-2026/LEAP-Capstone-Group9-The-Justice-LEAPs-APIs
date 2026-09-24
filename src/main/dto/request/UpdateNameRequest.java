package main.dto.request;

public record UpdateNameRequest(
    Integer userId,
    String name
) {}
