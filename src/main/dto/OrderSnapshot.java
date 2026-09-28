package main.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;


public class OrderSnapshot {
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("side")
    private String side;
    
    @JsonProperty("quantity")
    private Integer quantity;
    
    @JsonProperty("totalPrice")
    @JsonAlias("total_price")
    private BigDecimal totalPrice;
    
    @JsonProperty("instrumentId")
    @JsonAlias("instrument_id")
    private Integer instrumentId;
    
    @JsonProperty("accountId")
    @JsonAlias("account_id")
    private Integer accountId;
    
    @JsonProperty("updatedAt")
    @JsonAlias("updated_at")
    private LocalDateTime updatedAt;


   
    public OrderSnapshot() {
    }

    public OrderSnapshot(String status, String side, Integer quantity, BigDecimal totalPrice,
                         Integer instrumentId, Integer accountId, LocalDateTime updatedAt) {
        this.status = status;
        this.side = side;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.instrumentId = instrumentId;
        this.accountId = accountId;
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Integer instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
