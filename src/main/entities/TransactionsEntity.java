package main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.cglib.core.Local;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity 
@Table(name = "transactions")
public class TransactionsEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer transactionId;
    @JsonProperty("amount")
    @Column(name = "amount")
    private BigDecimal amount;
    @JsonProperty("side")
    @Column(name = "side")
    private String side;
    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountsEntity accountId;
    @JsonProperty("transaction_type")
    @Column(name = "transaction_type")
    private String transactionType;
    @CreationTimestamp 
    @JsonProperty("happened_at")
    @Column(name = "happened_at")
    private LocalDateTime happenedAt;

    //Getters and Setters
    public Integer getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
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

