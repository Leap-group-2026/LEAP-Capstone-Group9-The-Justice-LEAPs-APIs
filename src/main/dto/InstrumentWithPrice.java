package main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Instrument data enriched with current market price.
 * Separates the reference instrument data from volatile price data.
 * Price and quoteTime may be null if no current price exists.
 */
public class InstrumentWithPrice {
    private Integer instrumentId;
    @JsonProperty("ticker")
    private String ticker;
    @JsonProperty("asset_type")
    private String assetType;
    @JsonProperty("asset_name")
    private String assetName;
    @JsonProperty("currency")
    private String currency;
    @Schema(description = "Current price from current_prices; null when the instrument has no price row (never 0)", nullable = true)
    @JsonProperty("price")
    private BigDecimal price; 
    @Schema(description = "When the market quoted the price, so callers can judge how stale it is; null when there is no price", nullable = true)
    @JsonProperty("quote_time")
    private OffsetDateTime quoteTime;  // nullable: no current_prices row; TIMESTAMPTZ in Postgres

  
    public InstrumentWithPrice() {}

    public InstrumentWithPrice(Integer instrumentId, String ticker, String assetType, String assetName, 
                               String currency, BigDecimal price, OffsetDateTime quoteTime) {
        this.instrumentId = instrumentId;
        this.ticker = ticker;
        this.assetType = assetType;
        this.assetName = assetName;
        this.currency = currency;
        this.price = price;
        this.quoteTime = quoteTime;
    }

 
    public Integer getInstrumentId() {
        return instrumentId;
    }

    public String getTicker() {
        return ticker;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OffsetDateTime getQuoteTime() {
        return quoteTime;
    }

  
    public void setInstrumentId(Integer instrumentId) {
        this.instrumentId = instrumentId;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setQuoteTime(OffsetDateTime quoteTime) {
        this.quoteTime = quoteTime;
    }
}
