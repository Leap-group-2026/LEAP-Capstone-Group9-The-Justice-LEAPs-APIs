package main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import com.fasterxml.jackson.annotation.JsonProperty;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import java.math.BigDecimal;


import java.time.LocalDateTime;

@Entity
@Table(name = "positions")
public class PositionsEntity {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer positionId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    @JsonProperty("account")
    private AccountsEntity account;

    @JsonProperty("quantity")
    @Column(name = "quantity")
    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    @JsonProperty("instrument")
    private InstrumentEntity instrument;

    @JsonProperty("opened_at")
    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @JsonProperty("closed_at")
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @JsonProperty("total_price")
    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @JsonProperty("average_price")
    @Column(name = "average_price")
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



