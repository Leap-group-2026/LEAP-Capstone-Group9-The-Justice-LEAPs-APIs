package main.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import main.entities.instrumentEntity;
import main.entities.accountsEntity;

@Entity
@Table(name = "orders")
public class OrderEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;

    @JsonProperty("side")
    @Column(name = "side", nullable = false)
    private String side;

    @JsonProperty("account_id")
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private accountsEntity accountId;

    @JsonProperty("instrument_id")
    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    private instrumentEntity instrumentId;

    @JsonProperty("status")
    @Column(name = "status", nullable = false)
    private String status;

    @JsonProperty("quantity")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @JsonProperty("total_price")
    @Column(name = "total_price", nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public OrderEntity() {
    }

    public OrderEntity(String side, accountsEntity accountId, instrumentEntity instrumentId,
                       Integer quantity, BigDecimal totalPrice) {
        this.side = side;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;

        this.status = "PENDING";
    }

    public Integer getOrderId() { return orderId; }

    public String getSide() { return side; }

    public void setSide(String side) { this.side = side; }

    public accountsEntity getAccountId() { return accountId; }

    public void setAccountId(accountsEntity accountId) { this.accountId = accountId; }

    public instrumentEntity getInstrumentId() { return instrumentId; }

    public void setInstrumentId(instrumentEntity instrumentId) { this.instrumentId = instrumentId; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Integer getQuantity() { return quantity; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getTotalPrice() { return totalPrice; }

    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
