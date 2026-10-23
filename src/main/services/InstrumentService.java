package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.pagehelper.PageHelper;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import repos.InstrumentRepo;
import dto.InstrumentWithPrice;
import dto.response.NewsArticle;
import entities.InstrumentEntity;
import exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class InstrumentService {
    private InstrumentRepo repo;
    private final RestTemplate template = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Dotenv dotenv;

    public InstrumentService(InstrumentRepo repo){
        this.repo = repo;
        try {
            this.dotenv = Dotenv.load();
        } catch (DotenvException e) {
            this.dotenv = null;
        }
    }

    private String getEnvValue(String key) {
        if (dotenv != null) {
            String value = dotenv.get(key);
            if (value != null) {
                return value;
            }
        }
        return System.getenv(key);
    }

    public InstrumentEntity saveInstrument(InstrumentEntity entity) {
        if (repo.existsByTicker(entity.getTicker())) {
            throw new IllegalArgumentException("Ticker already exists");
        }
        try {
            repo.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Ticker already exists");
        }
        return entity;
    }

    public InstrumentWithPrice getTickerPrice(String ticker) {
        return repo.findByTicker(ticker).orElseThrow(() -> new ResourceNotFoundException("Instrument", ticker));
    }

    public InstrumentWithPrice getForexPrice(String currencyPair) {
        if (currencyPair == null || !currencyPair.matches("[A-Z]{3}/[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency pair must use the format USD/GBP");
        }
        return repo.findForexByTicker(currencyPair)
            .orElseThrow(() -> new ResourceNotFoundException("Foreign exchange pair", currencyPair));
    }

    public List<InstrumentWithPrice> getAllInstruments(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return repo.findAll();
    }

    public List<BigDecimal> getBarNumbers(String symbols, String timeframe, String start, String end) {
        String url = UriComponentsBuilder
            .fromHttpUrl("https://data.alpaca.markets/v2/stocks/bars")
            .queryParam("symbols", symbols)
            .queryParam("timeframe", timeframe)
            .queryParam("start", start)
            .queryParam("end", end)
            .queryParam("feed", "iex")
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", getEnvValue("ALPACA_KEY"));
        headers.set("APCA-API-SECRET-KEY", getEnvValue("ALPACA_SECRET"));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode barsBySymbol = objectMapper.readTree(response.getBody()).path("bars");
            List<BigDecimal> closingPrices = new ArrayList<>();

            barsBySymbol.elements().forEachRemaining(bars -> bars.forEach(bar -> {
                JsonNode close = bar.get("c");
                if (close != null && !close.isNull()) {
                    closingPrices.add(close.decimalValue());
                }
            }));

            return closingPrices;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to retrieve stock bars from Alpaca", e);
        }
    }

    public List<BigDecimal> getCryptoBarNumbers(String symbols, String timeframe, String start, String end) {
        symbols = UriUtils.decode(symbols, java.nio.charset.StandardCharsets.UTF_8);
        String url = UriComponentsBuilder
            .fromHttpUrl("https://data.alpaca.markets/v1beta3/crypto/us/bars")
            .queryParam("symbols", symbols)
            .queryParam("timeframe", timeframe)
            .queryParam("start", start)
            .queryParam("end", end)
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", getEnvValue("ALPACA_KEY"));
        headers.set("APCA-API-SECRET-KEY", getEnvValue("ALPACA_SECRET"));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode barsBySymbol = objectMapper.readTree(response.getBody()).path("bars");
            List<BigDecimal> closingPrices = new ArrayList<>();

            barsBySymbol.elements().forEachRemaining(bars -> bars.forEach(bar -> {
                JsonNode close = bar.get("c");
                if (close != null && !close.isNull()) {
                    closingPrices.add(close.decimalValue());
                }
            }));

            return closingPrices;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to retrieve crypto bars from Alpaca", e);
        }
    }

    public List<NewsArticle> getNews() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", getEnvValue("ALPACA_KEY"));
        headers.set("APCA-API-SECRET-KEY", getEnvValue("ALPACA_SECRET"));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = template.exchange(
                "https://data.alpaca.markets/v1beta1/news?limit=10",
                HttpMethod.GET,
                entity,
                String.class);
            JsonNode news = objectMapper.readTree(response.getBody()).path("news");
            List<NewsArticle> articles = new ArrayList<>();

            news.forEach(article -> articles.add(new NewsArticle(
                article.path("author").asText(),
                article.path("headline").asText(),
                article.path("url").asText())));

            return articles;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to retrieve market news from Alpaca", e);
        }
    }
}
