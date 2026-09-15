package main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Entity
@Table(name = "historical_orders")
public class historicalOrdersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer historicalOrderId;
    @JsonProperty("order_id")
    @ManyToOne 
    @JoinColumn(name = "order_id", nullable = false) //References orders(order_id)
    private Integer orderId;
    @JsonProperty("account_id")
    @ManyToOne 
    @JoinColumn(name = "account_id", nullable = false) //References accounts(account_id)
    private Integer accountId;
    @JsonProperty("order_information_json")
    @Column(name = "order_information_json", nullable = false)
    private String orderInformationJson;
    @JsonProperty("created_at")
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Getters and Setters
    public Integer getHistoricalOrderId() {
        return historicalOrderId;
    }

    public void setHistoricalOrderId(Integer historicalOrderId) {
        this.historicalOrderId = historicalOrderId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }


    public String getOrderInformationJson() {
        return orderInformationJson;
    }

    public void setOrderInformationJson(String orderInformationJson) {
        this.orderInformationJson = orderInformationJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
/*
    CREATE TABLE historical_orders(
        historical_order_id     SERIAL PRIMARY KEY,
        order_id                INTEGER NOT NULL REFERENCES orders(order_id),
        account_id              INTEGER NOT NULL REFERENCES accounts(account_id),
        order_information_json  JSONB NOT NULL,
        created_at              TIMESTAMP NOT NULL DEFAULT now()
    );
*/