package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class InstrumentEntity {
    private Integer instrumentId;
    @JsonProperty("ticker")
    private String ticker;
    @JsonProperty("asset_type")
    private String assetType;
    @JsonProperty("asset_name")
    private String assetName;
    @JsonProperty("price")
    private BigDecimal price;
    @JsonProperty("currency")
    private String currency;

    // Getters and Setters
    public Integer getInstrumentId() {
        return instrumentId;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
