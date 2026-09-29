package main.services;

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
import main.dto.request.TradesResponse;
import org.springframework.scheduling.annotation.Scheduled;
import main.repos.OrdersRepo;
import main.repos.CurrentPriceRepo;
import main.repos.InstrumentRepo;
import main.entities.CurrentPriceEntity;
import main.entities.OrderEntity;
import main.dto.request.TradeData;
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
        logger.info("Starting Alpaca API poll scheduled job");
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.set("APCA-API-KEY-ID", getEnvValue("ALPACA_KEY"));
            headers.set("APCA-API-SECRET-KEY", getEnvValue("ALPACA_SECRET"));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Top 50 S&P 500 stocks by market cap
            String symbols = "NVDA,MSFT,AAPL,GOOGL,AMZN,META,TSLA,BRK.B,V,JNJ,WMT,XOM,JPM,PG,MA,HD,NFLX,KO,BAC,PEP,CSCO,DIS,VZ,MRK,AXP,ADBE,WBA,CRM,IBM,INTC,QCOM,TXN,CMG,COST,CVX,LLY,HON,UNH,CAT,BA,MMM,NOC,CCI,SLB,USB,WFC,BLK,SO,EOG,PSX,OXY";
            String url = "https://data.alpaca.markets/v2/stocks/trades/latest?symbols=" + symbols;
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);

            if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null){
                logger.info("Successfully received response from Alpaca API");
                TradesResponse tradesResponse = objectMapper.readValue(response.getBody(), TradesResponse.class);
                processData(tradesResponse);
            }
        }
        catch(Exception e){
            logger.error("Error polling alpaca api", e);
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
        logger.info("Successfully processed and upserted {} price records", recordsProcessed);
    }
}