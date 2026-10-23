package dto.response;

import java.util.List;

public record AccountNewsResponse(
        Integer accountId,
        List<String> symbols,
        String sentiment,
        List<NewsArticleResponse> articles) {
}