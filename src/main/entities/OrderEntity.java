package main.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import main.entities.instrumentEntity;
import main.entities.accountsEntity;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@Getter
public class OrderEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;

    @Setter
    @JsonProperty("side")
    @Column(name = "side", nullable = false)
    private String side;

    @Setter
    @JsonProperty("account_id")
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private accountsEntity accountId;

    @Setter
    @JsonProperty("instrument_id")
    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    private instrumentEntity instrumentId;

    @Setter
    @JsonProperty("status")
    @Column(name = "status", nullable = false)
    private String status;

    @Setter
    @JsonProperty("quantity")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Setter
    @JsonProperty("total_price")
    @Column(name = "total_price", nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public OrderEntity(String side, accountsEntity accountId, instrumentEntity instrumentId,
                       Integer quantity, BigDecimal totalPrice) {
        this.side = side;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }
}
