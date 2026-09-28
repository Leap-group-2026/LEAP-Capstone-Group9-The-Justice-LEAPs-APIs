package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;


public class CurrentPriceEntity {
    private Integer instrumentId;
    
    @Schema(description = "Last traded price from the market data provider; always greater than 0")
    @JsonProperty("price")
    private BigDecimal price;
    
    @Schema(description = "When the market quoted this price (the price's own timestamp, UTC offset included)")
    @JsonProperty("quote_time")
    private OffsetDateTime quoteTime;
    
    @Schema(description = "When our price refresher fetched the quote; retrieved_at minus quote_time is the data lag")
    @JsonProperty("retrieved_at")
    private OffsetDateTime retrievedAt;


    public CurrentPriceEntity() {}

    public CurrentPriceEntity(Integer instrumentId, BigDecimal price, OffsetDateTime quoteTime, OffsetDateTime retrievedAt) {
        this.instrumentId = instrumentId;
        this.price = price;
        this.quoteTime = quoteTime;
        this.retrievedAt = retrievedAt;
    }


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
