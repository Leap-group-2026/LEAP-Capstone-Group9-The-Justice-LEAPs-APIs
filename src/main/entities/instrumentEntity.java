package main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "instruments")
public class instrumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer instrumentId;
    @JsonProperty("ticker")
    @Column(name = "ticker")
    private String ticker;
    @JsonProperty("asset_type")
    @Column(name = "asset_type")
    private String assetType;
    @JsonProperty("asset_name")
    @Column(name = "asset_name")
    private String assetName;
    @JsonProperty("price")
    @Column(name = "price")
    private BigDecimal price;
    @JsonProperty("currency")
    @Column(name = "currency")
    private String currency;

    // Getters and Setters
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
