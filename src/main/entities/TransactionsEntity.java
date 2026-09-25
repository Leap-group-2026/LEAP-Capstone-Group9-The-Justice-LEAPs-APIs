package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionsEntity{
    private Integer transactionId;
    @JsonProperty("amount")
    private BigDecimal amount;
    @JsonProperty("side")
    private String side;
    private AccountsEntity accountId;
    @JsonProperty("transaction_type")
    private String transactionType;
    @JsonProperty("happened_at")
    private LocalDateTime happenedAt;

    //Getters and Setters
    public Integer getTransactionId() {
        return transactionId;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public String getSide() {
        return side;
    }
    public void setSide(String side) {
        this.side = side;
    }
    public AccountsEntity getAccountId() {
        return accountId;
    }
    public void setAccountId(AccountsEntity accountId) {
        this.accountId = accountId;
    }
    public String getTransactionType() {
        return transactionType;
    }
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    public LocalDateTime getHappenedAt() {
        return happenedAt;
    }
    public void setHappenedAt(LocalDateTime happenedAt) {
        this.happenedAt = happenedAt;
    }
}

