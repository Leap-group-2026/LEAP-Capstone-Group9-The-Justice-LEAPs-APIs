package dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {
    private Integer accountId;
    private Integer userId;
    private BigDecimal balance;
    private String portfolioSize;
    private String tradeType;
    private LocalDateTime createdAt;
    private Boolean accountActive;

    public AccountResponse(Integer accountId, Integer userId, BigDecimal balance, String portfolioSize, String tradeType, LocalDateTime createdAt, Boolean accountActive) {
        this.accountId = accountId;
        this.userId = userId;
        this.balance = balance;
        this.portfolioSize = portfolioSize;
        this.tradeType = tradeType;
        this.createdAt = createdAt;
        this.accountActive = accountActive;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getPortfolioSize() {
        return portfolioSize;
    }

    public void setPortfolioSize(String portfolioSize) {
        this.portfolioSize = portfolioSize;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
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
