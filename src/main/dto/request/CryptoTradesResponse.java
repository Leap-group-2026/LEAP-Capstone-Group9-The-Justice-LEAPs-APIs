package main.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class CryptoTradesResponse {
    @JsonProperty("trades")
    private Map<String, CryptoTradeData> trades;

    public Map<String, CryptoTradeData> getTrades() { return trades; }
    public void setTrades(Map<String, CryptoTradeData> trades) { this.trades = trades; }
}
