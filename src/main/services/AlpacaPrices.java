package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import dto.request.TradesResponse;
import dto.request.CryptoTradesResponse;
import dto.request.CryptoTradeData;
import dto.request.ForexRatesResponse;
import org.springframework.scheduling.annotation.Scheduled;
import repos.OrdersRepo;
import repos.CurrentPriceRepo;
import repos.InstrumentRepo;
import entities.CurrentPriceEntity;
import entities.OrderEntity;
import dto.request.TradeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AlpacaPrices {
    
    private static final Logger logger = LoggerFactory.getLogger(AlpacaPrices.class);
    
    @Autowired
    private CurrentPriceRepo currentPriceRepo;
    
    @Autowired
    private InstrumentRepo instrumentRepo;
    
    private Dotenv dotenv;
    private final RestTemplate template = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public AlpacaPrices() {
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
    
    @Scheduled(fixedRate = 10000)
    public void pollAlpaca(){
        logger.info("Starting Alpaca API stock poll scheduled job");
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.set("APCA-API-KEY-ID", getEnvValue("ALPACA_KEY"));
            headers.set("APCA-API-SECRET-KEY", getEnvValue("ALPACA_SECRET"));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String symbols = "NVDA,MSFT,AAPL,GOOGL,AMZN,META,TSLA,BRK.B,V,JNJ,WMT,XOM,JPM,PG,MA,HD,NFLX,KO,BAC,PEP,CSCO,DIS,VZ,MRK,AXP,ADBE,WBA,CRM,IBM,INTC,QCOM,TXN,CMG,COST,CVX,LLY,HON,UNH,CAT,BA,MMM,NOC,CCI,SLB,USB,WFC,BLK,SO,EOG,PSX,OXY";
            String url = "https://data.alpaca.markets/v2/stocks/trades/latest?symbols=" + symbols;
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);

            if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null){
                logger.info("Successfully received response from Alpaca stock API");
                TradesResponse tradesResponse = objectMapper.readValue(response.getBody(), TradesResponse.class);
                processData(tradesResponse);
            }
        }
        catch(Exception e){
            logger.error("Error polling alpaca stock api", e);
        }
    }

    @Scheduled(fixedRate = 10000)
    public void pollCrypto(){
        logger.info("Starting Alpaca API crypto poll scheduled job");
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.set("APCA-API-KEY-ID", getEnvValue("ALPACA_KEY"));
            headers.set("APCA-API-SECRET-KEY", getEnvValue("ALPACA_SECRET"));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String loc = "us";
            String symbols = "BTC/USD,ETH/USD,SOL/USD,XRP/USD,BNB/USD,DOGE/USD,ADA/USD,AVAX/USD,LINK/USD,LTC/USD";
            String url = "https://data.alpaca.markets/v1beta3/crypto/" + loc + "/latest/trades?symbols=" + symbols;
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);

            if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null){
                logger.info("Successfully received response from Alpaca crypto API");
                CryptoTradesResponse cryptoTradesResponse = objectMapper.readValue(response.getBody(), CryptoTradesResponse.class);
                processData(cryptoTradesResponse);
            }
        }
        catch(Exception e){
            logger.error("Error polling alpaca crypto api", e);
        }
    }

    @Scheduled(cron = "0 0 16 * * *", zone = "America/Chicago")
    public void pollForex(){
        logger.info("Starting Frankfurter forex poll scheduled job");
        try{
            String url = "https://api.frankfurter.dev/v2/rates?base=USD&quotes=EUR,GBP,INR,JPY,CAD,AUD";
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);

            if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null){
                logger.info("Successfully received response from Frankfurter forex API");
                ForexRatesResponse forexRatesResponse = objectMapper.readValue(response.getBody(), ForexRatesResponse.class);
                processData(forexRatesResponse);
            }
        }
        catch(Exception e){
            logger.error("Error polling frankfurter forex api", e);
        }
    }

    public void processData(TradesResponse tradesResponse){
        int recordsProcessed = 0;
        for(Map.Entry<String, TradeData> entry : tradesResponse.getTrades().entrySet()){
            String symbol = entry.getKey();
            BigDecimal price = entry.getValue().getPrice();
            String quoteTime = entry.getValue().getTimestamp();

            Integer instrumentId = instrumentRepo.findIdBySymbol(symbol);
            if (instrumentId != null) {
                CurrentPriceEntity currentPriceEntity = new CurrentPriceEntity(instrumentId, price, OffsetDateTime.parse(quoteTime), OffsetDateTime.now());
                currentPriceRepo.upsert(currentPriceEntity);
                recordsProcessed++;
            }
        }
        logger.info("Successfully processed and upserted {} stock price records", recordsProcessed);
    }

    public void processData(CryptoTradesResponse cryptoTradesResponse){
        int recordsProcessed = 0;
        for(Map.Entry<String, CryptoTradeData> entry : cryptoTradesResponse.getTrades().entrySet()){
            String symbol = entry.getKey();
            BigDecimal price = entry.getValue().getPrice();
            String quoteTime = entry.getValue().getTimestamp();

            Integer instrumentId = instrumentRepo.findIdBySymbol(symbol);
            if (instrumentId != null) {
                CurrentPriceEntity currentPriceEntity = new CurrentPriceEntity(instrumentId, price, OffsetDateTime.parse(quoteTime), OffsetDateTime.now());
                currentPriceRepo.upsert(currentPriceEntity);
                recordsProcessed++;
            }
        }
        logger.info("Successfully processed and upserted {} crypto price records", recordsProcessed);
    }

    public void processData(ForexRatesResponse forexRatesResponse){
        int recordsProcessed = 0;
        logger.info("Processing forex rates");
        for(ForexRatesResponse.ForexRate forexRate : forexRatesResponse){
            String currencyPair = forexRate.getBase() + "/" + forexRate.getQuote();
            BigDecimal rate = BigDecimal.valueOf(forexRate.getRate());
            String quoteTime = forexRate.getDate();

            Integer instrumentId = instrumentRepo.findIdBySymbol(currencyPair);
            if (instrumentId != null) {
                CurrentPriceEntity currentPriceEntity = new CurrentPriceEntity(instrumentId, rate, OffsetDateTime.parse(quoteTime + "T00:00:00Z"), OffsetDateTime.now());
                currentPriceRepo.upsert(currentPriceEntity);
                recordsProcessed++;
            }
        }
        logger.info("Successfully processed and upserted {} forex price records", recordsProcessed);
    }
}
