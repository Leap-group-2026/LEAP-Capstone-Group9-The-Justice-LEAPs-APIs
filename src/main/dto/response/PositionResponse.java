package main.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PositionResponse {

    @Schema(description = "The unique identifier of the position")
    @JsonProperty("position_id")
    private Integer positionId;
    
    @Schema(description = "The unique identifier of the account associated with the position")
    @JsonProperty("account_id")
    private Integer accountId;

    @Schema(description = "The unique identifier of the instrument associated with the position")
    @JsonProperty("instrument_id")
    private Integer instrumentId;

    @Schema(description = "The ticker symbol of the instrument")
    @JsonProperty("ticker")
    private String ticker;

    @Schema(description = "The name of the asset")
    @JsonProperty("asset_name")
    private String assetName;

    @Schema(description = "The type of the asset")
    @JsonProperty("asset_type")
    private String assetType;

    @Schema(description = "The currency of the position")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "The quantity of the asset in the position")
    @JsonProperty("quantity")
    private Integer quantity;

    @Schema(description = "The average price of the asset in the position")
    @JsonProperty("average_price")
    private BigDecimal averagePrice;

    @Schema(description = "The total price of the asset in the position")
    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @Schema(description = "The date and time when the position was opened")
    @JsonProperty("opened_at")
    private LocalDateTime openedAt;

    public PositionResponse() {
    }

    public Integer getPositionId() {
        return positionId;
    }

    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

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

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }
}
