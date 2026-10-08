package dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderAdminResponse {
    @JsonProperty("order_id")
    private Integer orderId;

    @JsonProperty("account_id")
    private Integer accountId;

    @JsonProperty("instrument_id")
    private Integer instrumentId;

    @JsonProperty("side")
    private String side;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    public OrderAdminResponse() {
}

    public OrderAdminResponse(String side, Integer accountId, Integer instrumentId,
                       Integer quantity, BigDecimal totalPrice) {
        this.side = side;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = "PENDING";
    }

    public Integer getOrderId() { return orderId; }
    
    public void setOrderId(Integer orderId) { 
        this.orderId = orderId; 
    }
    

    public String getSide() { 
        return side; 
    }

    public void setSide(String side) { 
        this.side = side;

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


    public String getStatus() { 
        return status; 
    }

    public void setStatus(String status) { 
        this.status = status; 
    }

    public Integer getQuantity() { 
        return quantity; 
    }

    public void setQuantity(Integer quantity) {
         this.quantity = quantity; 
        }

    public BigDecimal getTotalPrice() { 
        return totalPrice;
     }

    public void setTotalPrice(BigDecimal totalPrice) { 
        this.totalPrice = totalPrice;
     }

    public LocalDateTime getCreatedAt() {
         return createdAt; 
        }

    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }

    public LocalDateTime getUpdatedAt() { 
        return updatedAt; 
    }

    public void setUpdatedAt(LocalDateTime updatedAt) { 
        this.updatedAt = updatedAt; 
    }
}
