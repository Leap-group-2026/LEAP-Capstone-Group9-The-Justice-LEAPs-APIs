// Create new file: src/main/dto/response/TradeData.java
package main.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class TradeData {
    @JsonProperty("c")
    private List<String> conditions;
    
    @JsonProperty("i")
    private int sequenceNumber;
    
    @JsonProperty("p")
    private BigDecimal price;
    
    @JsonProperty("s")
    private int size;
    
    @JsonProperty("t")
    private String timestamp;
    
    @JsonProperty("x")
    private String exchange;
    
    @JsonProperty("z")
    private String tape;

    public List<String> getConditions() { return conditions; }
    public void setConditions(List<String> conditions) { this.conditions = conditions; }
    
    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    
    public String getTape() { return tape; }
    public void setTape(String tape) { this.tape = tape; }
}