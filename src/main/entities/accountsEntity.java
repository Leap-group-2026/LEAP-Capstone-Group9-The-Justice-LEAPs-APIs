package main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class accountsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accountId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonProperty("user")
    private userEntity user;

    @JsonProperty("balance")
    @Column(name = "balance")
    private BigDecimal balance;

    @JsonProperty("portfolio_size")
    @Column(name = "portfolio_size", nullable = false)
    @Convert(converter = PortfolioSizeConverter.class)
    private PortfolioSize portfolioSize;

    @JsonProperty("trade_type")
    @Column(name = "trade_type")
    private String trade_type;

    @CreationTimestamp 
    @JsonProperty("created_at")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public userEntity getUserId() {
        return user;
    }

    public void setUserId(userEntity user) {
        this.user = user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public PortfolioSize getPortfolioSize() {
        return portfolioSize;
    }

    public void setPortfolioSize(PortfolioSize portfolioSize) {
        this.portfolioSize = portfolioSize;
    }

    public String getTradeType() {
        return trade_type;
    }

    public void setTradeType(String trade_type) {
        this.trade_type = trade_type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }



    }

    

