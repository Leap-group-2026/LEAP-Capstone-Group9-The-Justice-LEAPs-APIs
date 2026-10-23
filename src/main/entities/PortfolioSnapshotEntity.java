package entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioSnapshotEntity {
    private Integer portfolioSnapshotId;
    private Integer accountId;
    private BigDecimal portfolioValue;
    private LocalDate snapshotDate;

    public Integer getPortfolioSnapshotId() {
        return portfolioSnapshotId;
    }

    public void setPortfolioSnapshotId(Integer portfolioSnapshotId) {
        this.portfolioSnapshotId = portfolioSnapshotId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public void setPortfolioValue(BigDecimal portfolioValue) {
        this.portfolioValue = portfolioValue;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }
}