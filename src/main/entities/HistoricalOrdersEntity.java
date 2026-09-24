package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class HistoricalOrdersEntity {
    private Integer historicalOrderId;
    @JsonProperty("order_id")
    private OrderEntity orderId;
    private AccountsEntity account;
    @JsonProperty("order_information_json")
    private String orderInformationJson;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Getters and Setters
    public Integer getHistoricalOrderId() {
        return historicalOrderId;
    }

    public void setHistoricalOrderId(Integer historicalOrderId) {
        this.historicalOrderId = historicalOrderId;
    }

    public OrderEntity getOrderId() {
        return orderId;
    }

    public void setOrderId(OrderEntity orderId) {
        this.orderId = orderId;
    }

    public AccountsEntity getAccount() {
        return account;
    }

    public void setAccount(AccountsEntity account) {
        this.account = account;
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