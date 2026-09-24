package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PositionsEntity {
    private Integer positionId;

    @JsonProperty("account")
    private AccountsEntity account;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("instrument")
    private InstrumentEntity instrument;

    @JsonProperty("opened_at")
    private LocalDateTime openedAt;

    @JsonProperty("closed_at")
    private LocalDateTime closedAt;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @JsonProperty("average_price")
    private BigDecimal averagePrice;

    public Integer getPositionId() {
        return positionId;
    }

    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }

    public AccountsEntity getAccountId() {
        return account;
    }

    public void setAccountId(AccountsEntity account) {
        this.account = account;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public InstrumentEntity getInstrumentId() {
        return instrument;
    }

    public void setInstrumentId(InstrumentEntity instrument) {
        this.instrument = instrument;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }
}



