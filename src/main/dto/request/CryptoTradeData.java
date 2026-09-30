package main.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class CryptoTradeData {
    @JsonProperty("i")
    private long tradeId;
    
    @JsonProperty("p")
    private BigDecimal price;
    
    @JsonProperty("s")
    private BigDecimal size;
    
    @JsonProperty("t")
    private String timestamp;
    
    @JsonProperty("tks")
    private String takerSide;

    public long getTradeId() { return tradeId; }
    public void setTradeId(long tradeId) { this.tradeId = tradeId; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getSize() { return size; }
    public void setSize(BigDecimal size) { this.size = size; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getTakerSide() { return takerSide; }
    public void setTakerSide(String takerSide) { this.takerSide = takerSide; }
}
