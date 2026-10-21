package dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record NewsArticleResponse(
        String uuid,
        String title,
        String description,
        String url,
        String source,
        String imageUrl,
        OffsetDateTime publishedAt,
        String sentiment,
        Double sentimentScore,
        List<String> symbols) {
}