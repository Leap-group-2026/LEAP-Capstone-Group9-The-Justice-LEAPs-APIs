package dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderAccountResponse {

    @JsonProperty("instrument")
    private String instrument;

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
    
    public OrderAccountResponse() {
}

    public OrderAccountResponse(String side, String instrument,
                       Integer quantity, BigDecimal totalPrice) {
        this.side = side;
        this.instrument = instrument;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = "PENDING";
    }

   
    

    public String getSide() { 
        return side; 
    }

    public void setSide(String side) { 
        this.side = side;

    }



    public String getInstrument() { 
        return instrument; 
    }

    public void setInstrument(String instrument) { 
        this.instrument = instrument; 
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
