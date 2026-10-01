
package dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class TradesResponse {
    @JsonProperty("trades")
    private Map<String, TradeData> trades;

    public Map<String, TradeData> getTrades() { return trades; }
    public void setTrades(Map<String, TradeData> trades) { this.trades = trades; }
}
