package main.dto.request;

public record UpdateAddressRequest(
    Integer userId,
    String address
) {}
