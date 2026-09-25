package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class OrderEntity {
    
    private Integer orderId;

    @JsonProperty("side")
    private String side;

    @JsonProperty("account_id")
    private AccountsEntity accountId;

    @JsonProperty("instrument_id")
    private InstrumentEntity instrumentId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public OrderEntity() {
    }

    public OrderEntity(String side, AccountsEntity accountId, InstrumentEntity instrumentId,
                       Integer quantity, BigDecimal totalPrice) {
        this.side = side;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = "PENDING";
    }

    public Integer getOrderId() { return orderId; }
    
    // DO NOT REMOVE: Setter is required for MyBatis to attach db-generated id to entities
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getSide() { return side; }

    public void setSide(String side) { this.side = side; }

    public AccountsEntity getAccountId() { return accountId; }

    public void setAccountId(AccountsEntity accountId) { this.accountId = accountId; }

    public InstrumentEntity getInstrumentId() { return instrumentId; }

    public void setInstrumentId(InstrumentEntity instrumentId) { this.instrumentId = instrumentId; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Integer getQuantity() { return quantity; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getTotalPrice() { return totalPrice; }

    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
