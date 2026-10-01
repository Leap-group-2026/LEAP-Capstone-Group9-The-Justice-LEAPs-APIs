package dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderHistoryResponse {
    @Schema(description = "The unique identifier of the order")
    @JsonProperty("orderId")
    private Integer orderId;

    @Schema(description = "The ticker symbol of the instrument")
    @JsonProperty("ticker")
    private String ticker;

    @Schema(description = "The order type: BUY or SELL")
    @JsonProperty("side")
    private String side;

    @Schema(description = "The number of units ordered")
    @JsonProperty("quantity")
    private Integer quantity;

    @Schema(description = "The price per unit (totalPrice / quantity)")
    @JsonProperty("pricePerUnit")
    private BigDecimal pricePerUnit;

    @Schema(description = "The order status: PENDING, FILLED, DECLINED, FAILED or CANCELED")
    @JsonProperty("status")
    private String status;

    @Schema(description = "The total value of the order")
    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;

    @Schema(description = "When the order was executed. Null unless status is FILLED.", nullable = true)
    @JsonProperty("executedAt")
    private LocalDateTime executedAt;

    public OrderHistoryResponse() {
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
