package dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

public class AccountResponse {
    private Integer accountId;
    private Integer userId;
    private BigDecimal balance;
    private String portfolioSize;
    private String tradeType;
    private LocalDateTime createdAt;
    private Boolean accountActive;
    private Boolean accountFrozen;
    @Schema(description = "Name the client gave the account; null when it has none", nullable = true)
    private String accountName;

    public AccountResponse(Integer accountId, Integer userId, BigDecimal balance, String portfolioSize, String tradeType, LocalDateTime createdAt, Boolean accountActive, Boolean accountFrozen, String accountName) {
        this.accountId = accountId;
        this.userId = userId;
        this.balance = balance;
        this.portfolioSize = portfolioSize;
        this.tradeType = tradeType;
        this.createdAt = createdAt;
        this.accountActive = accountActive;
        this.accountFrozen = accountFrozen;
        this.accountName = accountName;
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

    public Boolean getAccountFrozen() {
        return accountFrozen;
    }

    public void setAccountFrozen(Boolean accountFrozen) {
        this.accountFrozen = accountFrozen;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
}
