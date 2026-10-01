package entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InstrumentEntity {
    private Integer instrumentId;
    @JsonProperty("ticker")
    private String ticker;
    @JsonProperty("asset_type")
    private String assetType;
    @JsonProperty("asset_name")
    private String assetName;
    @JsonProperty("currency")
    private String currency;


    public Integer getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Integer instrumentId) {
        this.instrumentId = instrumentId;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
