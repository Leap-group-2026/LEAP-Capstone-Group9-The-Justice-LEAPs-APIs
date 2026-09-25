package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountsEntity {

    private Integer accountId;

    @JsonProperty("user")
    private UserEntity user;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("portfolio_size")
    private PortfolioSize portfolioSize;

    @JsonProperty("trade_type")
    private String trade_type;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("account_active")
    private Boolean accountActive;
    public Integer getAccountId() {
        return accountId;
    }

    public UserEntity getUserId() {
        return user;
    }

    public void setUserId(UserEntity user) {
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
    public Boolean getAccountActive() {
        return accountActive;
    }

    public void setAccountActive(Boolean accountActive) {
        this.accountActive = accountActive;
    }
    }

    

