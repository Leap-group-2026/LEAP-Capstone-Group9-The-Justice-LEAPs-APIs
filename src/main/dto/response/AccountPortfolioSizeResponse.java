package dto.response;

public record AccountPortfolioSizeResponse(
    Integer accountId,
    Integer userId,
    String portfolioSize
) {}