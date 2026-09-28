package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Current market price for an instrument.
 * Separate from InstrumentEntity to allow price to be updated
 * independently and tracked temporally via retrieved_at.
 */
public class CurrentPriceEntity {
    private Integer instrumentId;
    
    @JsonProperty("price")
    private BigDecimal price;
    
    @JsonProperty("quote_time")
    private OffsetDateTime quoteTime;
    
    @JsonProperty("retrieved_at")
    private OffsetDateTime retrievedAt;

    // Constructors
    public CurrentPriceEntity() {}

    public CurrentPriceEntity(Integer instrumentId, BigDecimal price, OffsetDateTime quoteTime, OffsetDateTime retrievedAt) {
        this.instrumentId = instrumentId;
        this.price = price;
        this.quoteTime = quoteTime;
        this.retrievedAt = retrievedAt;
    }

    // Getters and Setters
    public Integer getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Integer instrumentId) {
        this.instrumentId = instrumentId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public OffsetDateTime getQuoteTime() {
        return quoteTime;
    }

    public void setQuoteTime(OffsetDateTime quoteTime) {
        this.quoteTime = quoteTime;
    }

    public OffsetDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(OffsetDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }
}
