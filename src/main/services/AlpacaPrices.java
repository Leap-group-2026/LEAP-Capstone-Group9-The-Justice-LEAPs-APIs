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
import main.dto.request.TradesResponse;
import org.springframework.scheduling.annotation.Scheduled;
import main.repos.OrdersRepo;
import main.entities.OrderEntity;
import main.dto.request.TradeData;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AlpacaPrices {
    
    private final Dotenv dotenv = Dotenv.load();
    private final RestTemplate template = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Scheduled(fixedRate = 10000)
    public void pollAlpaca(){
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.set("APCA-API-KEY-ID", dotenv.get("ALPACA_KEY"));
            headers.set("APCA-API-SECRET-KEY", dotenv.get("ALPACA_SECRET"));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://data.alpaca.markets/v2/stocks/trades/latest?symbols=AAPL,MSFT,TSLA";
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);

            if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null){
                TradesResponse tradesResponse = objectMapper.readValue(response.getBody(), TradesResponse.class);
                processData(tradesResponse);
            }
        }
        catch(Exception e){
            System.err.println("Error polling alpaca api");
            e.printStackTrace();
        }
    }

    public void processData(TradesResponse tradesResponse){
        for(Map.Entry<String, TradeData> entry : tradesResponse.getTrades().entrySet()){
            String symbol = entry.getKey();
            BigDecimal price = entry.getValue().getPrice();
            String quoteTime = entry.getValue().getTimestamp();
        }
    }
}