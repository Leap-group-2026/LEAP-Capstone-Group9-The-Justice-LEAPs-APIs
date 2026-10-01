package entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "false once the account is closed; closed accounts are never returned by the account endpoints")
    @JsonProperty("account_active")
    private Boolean accountActive;
    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }


    @JsonProperty("user")
    public UserEntity getUserId() {
        return user;
    }

    @JsonProperty("user")
    public void setUserId(UserEntity user) {
        this.user = user;
    }

    @Schema(description = "Id of the owning user (read-only; send user.userId on input)")
    @JsonProperty(value = "userId", access = JsonProperty.Access.READ_ONLY)
    public Integer getOwnerUserId() {
        return user != null ? user.getUserId() : null;
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

    

