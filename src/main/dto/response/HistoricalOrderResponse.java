package main.dto.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public class HistoricalOrderResponse {
    @Schema(description = "Id of this history event")
    @JsonProperty("event_id")
    private Integer eventId;
    
    @JsonProperty("order_id")
    private Integer orderId;
    
    @JsonProperty("account_id")
    private Integer accountId;
    
    @Schema(description = "The stored snapshot exactly as recorded, as a raw JSON string")
    @JsonProperty("snapshot")
    private String snapshot;
    
    @Schema(description = "When this history event was recorded")
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
    
    @Schema(description = "The order's updated_at as captured in the snapshot (not when the event was recorded)")
    @JsonProperty("snapshot_updated_at")
    private LocalDateTime snapshotUpdatedAt;
    
    @Schema(description = "Set to \"Snapshot could not be read\" when the stored snapshot is unreadable; the snapshot fields "
        + "(status, side, quantity, total_price, instrument_id, snapshot_updated_at) are then null. Null when the snapshot was read normally.", nullable = true)
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
