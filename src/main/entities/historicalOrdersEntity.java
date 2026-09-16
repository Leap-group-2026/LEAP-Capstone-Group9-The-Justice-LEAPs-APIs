package entities;

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
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import java.time.LocalDateTime;

@Entity
@Table(name = "historical_orders")
public class historicalOrdersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer historicalOrderId;
    @JsonProperty("order_id")
    @Column(name = "order_id", nullable = false)
    private Integer orderId;
    @JsonProperty("account_id")
    @ManyToOne 
    @JoinColumn(name = "account_id", nullable = false) //References accounts(account_id)
    private accountsEntity accountId;
    @JsonProperty("order_information_json")
    @JdbcTypeCode(SqlTypes.JSON)
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

    public accountsEntity getAccountId() {
        return accountId;
    }

    public void setAccountId(accountsEntity accountId) {
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