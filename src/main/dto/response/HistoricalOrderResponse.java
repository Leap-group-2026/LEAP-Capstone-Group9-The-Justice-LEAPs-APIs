package main.dto.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HistoricalOrderResponse {
    @JsonProperty("event_id")
    private Integer eventId;
    
    @JsonProperty("order_id")
    private Integer orderId;
    
    @JsonProperty("account_id")
    private Integer accountId;
    
    @JsonProperty("snapshot")
    private String snapshot;
    
    @JsonProperty("occurred_at")
    private LocalDateTime occurredAt;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("side")
    private String side;
    
    @JsonProperty("quantity")
    private Integer quantity;
    
    @JsonProperty("total_price")
    private BigDecimal totalPrice;
    
    @JsonProperty("instrument_id")
    private Integer instrumentId;
    
    @JsonProperty("snapshot_updated_at")
    private LocalDateTime snapshotUpdatedAt;
    
    @JsonProperty("snapshot_error")
    private String snapshotError;  
    
    public HistoricalOrderResponse() {
    }
    
    public HistoricalOrderResponse(Integer eventId, Integer orderId, Integer accountId, String snapshot, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.accountId = accountId;
        this.snapshot = snapshot;
        this.occurredAt = occurredAt;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
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

    public LocalDateTime getSnapshotUpdatedAt() {
        return snapshotUpdatedAt;
    }

    public void setSnapshotUpdatedAt(LocalDateTime snapshotUpdatedAt) {
        this.snapshotUpdatedAt = snapshotUpdatedAt;
    }
    
    public String getSnapshotError() {
        return snapshotError;
    }
    
    public void setSnapshotError(String snapshotError) {
        this.snapshotError = snapshotError;
    }
}
