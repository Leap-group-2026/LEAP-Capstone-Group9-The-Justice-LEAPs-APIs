package main.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@Getter
public class OrderEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;
    
    @NotNull
    @Setter
    @JsonProperty("side")
    @Column(name = "side", nullable = false)
    private String side;
    
    @NotNull
    @Setter
    @JsonProperty("account_id")
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity accountId;
    
    @NotNull
    @Setter
    @JsonProperty("instrument_id")
    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    private InstrumentEntity instrumentId;
    
    @NotNull
    @Setter
    @JsonProperty("status")
    @Column(name = "status", nullable = false)
    private String status;
    
    @NotNull
    @Positive
    @Setter
    @JsonProperty("quantity")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @NotNull
    @Positive
    @Setter
    @JsonProperty("total_price")
    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Custom constructor for order creation (5 params)
    public OrderEntity(String side, AccountEntity accountId, InstrumentEntity instrumentId, 
                       Integer quantity, Double totalPrice) {
        this.side = side;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }
}
