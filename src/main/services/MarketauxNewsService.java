package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.response.AccountNewsResponse;
import dto.response.NewsArticleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import repos.PositionsRepo;
import services.resolver.AccountResolver;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MarketauxNewsService {
    private final PositionsRepo positionsRepo;
    private final AccountResolver accountResolver;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiToken;
    private final String baseUrl;

    public MarketauxNewsService(
            PositionsRepo positionsRepo,
            AccountResolver accountResolver,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${marketaux.api-token:}") String apiToken,
            @Value("${marketaux.base-url:https://api.marketaux.com/v1}") String baseUrl) {
        this.positionsRepo = positionsRepo;
        this.accountResolver = accountResolver;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiToken = apiToken;
        this.baseUrl = baseUrl;
    }

    public AccountNewsResponse findNews(Integer accountId, String requestedSentiment, Integer limit) {
        accountResolver.resolve(accountId);
        String sentiment = normalizeSentiment(requestedSentiment);
        int articleLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 100);

        Set<String> symbols = new LinkedHashSet<>();
        for (var position : positionsRepo.findOpenPositionsByAccountId(accountId)) {
            if (position.getTicker() != null && !position.getTicker().isBlank()) {
                symbols.add(position.getTicker().trim().toUpperCase(Locale.ROOT));
            }
        }

        if (symbols.isEmpty()) {
            return new AccountNewsResponse(accountId, List.of(), sentiment, List.of());
        }
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("MARKETAUX_API_TOKEN is not configured");
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl).path("/news/all")
                .queryParam("api_token", apiToken)
                .queryParam("symbols", String.join(",", symbols))
                .queryParam("language", "en")
                .queryParam("filter_entities", "true")
                .queryParam("limit", articleLimit)
                .build()
                .toUri();

        String body = restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
        return new AccountNewsResponse(accountId, List.copyOf(symbols), sentiment,
                parseArticles(body, sentiment, articleLimit));
    }

    private List<NewsArticleResponse> parseArticles(String body, String requestedSentiment, int limit) {
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            List<NewsArticleResponse> articles = new ArrayList<>();
            for (JsonNode article : data) {
                NewsArticleResponse parsed = parseArticle(article);
                if ("all".equals(requestedSentiment) || requestedSentiment.equals(parsed.sentiment())) {
                    articles.add(parsed);
                }
                if (articles.size() == limit) {
                    break;
                }
            }
            return articles;
        } catch (Exception exception) {
            throw new IllegalStateException("Marketaux returned an invalid news response", exception);
        }
    }

    private NewsArticleResponse parseArticle(JsonNode article) {
        List<String> symbols = new ArrayList<>();
        List<Double> entityScores = new ArrayList<>();
        for (JsonNode entity : article.path("entities")) {
            String symbol = entity.path("symbol").asText("");
            if (!symbol.isBlank()) {
                symbols.add(symbol);
            }
            if (entity.has("sentiment_score") && entity.get("sentiment_score").isNumber()) {
                entityScores.add(entity.get("sentiment_score").doubleValue());
            }
        }

        Double score = article.has("sentiment_score") && article.get("sentiment_score").isNumber()
                ? article.get("sentiment_score").doubleValue()
                : entityScores.stream().mapToDouble(Double::doubleValue).average().isPresent()
                    ? entityScores.stream().mapToDouble(Double::doubleValue).average().getAsDouble()
                    : 0.0;
        return new NewsArticleResponse(
                text(article, "uuid"),
                text(article, "title"),
                text(article, "description"),
                text(article, "url"),
                text(article, "source"),
                text(article, "image_url"),
                parseDate(text(article, "published_at")),
                sentimentFor(score),
                score,
                symbols);
    }

    private String normalizeSentiment(String sentiment) {
        if (sentiment == null || sentiment.isBlank() || "all".equalsIgnoreCase(sentiment)) {
            return "all";
        }
        String normalized = sentiment.toLowerCase(Locale.ROOT);
        if (!Set.of("positive", "neutral", "negative").contains(normalized)) {
            throw new IllegalArgumentException("sentiment must be positive, neutral, negative, or all");
        }
        return normalized;
    }

    private String sentimentFor(double score) {
        if (score > 0.15) {
            return "positive";
        }
        if (score < -0.15) {
            return "negative";
        }
        return "neutral";
    }

    private String text(JsonNode node, String field) {
        return node.path(field).isNull() ? null : node.path(field).asText(null);
    }

    private OffsetDateTime parseDate(String value) {
        return value == null || value.isBlank() ? null : OffsetDateTime.parse(value);
    }
}