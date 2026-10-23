package services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import entities.AccountsEntity;
import repos.AccountsRepo;
import repos.PortfolioSnapshotRepo;
import repos.PositionsRepo;
import entities.PortfolioSnapshotEntity;

@Service
public class PortfolioSnapshotService {
    private static final ZoneId SNAPSHOT_ZONE = ZoneId.of("America/Chicago");

    private final AccountsRepo accountsRepo;
    private final PositionsRepo positionsRepo;
    private final PortfolioSnapshotRepo portfolioSnapshotRepo;

    public PortfolioSnapshotService(AccountsRepo accountsRepo,
                                    PositionsRepo positionsRepo,
                                    PortfolioSnapshotRepo portfolioSnapshotRepo) {
        this.accountsRepo = accountsRepo;
        this.positionsRepo = positionsRepo;
        this.portfolioSnapshotRepo = portfolioSnapshotRepo;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "America/Chicago")
    public void createDailySnapshots() {
        LocalDate snapshotDate = LocalDate.now(SNAPSHOT_ZONE);

        for (AccountsEntity account : accountsRepo.findAll()) {
            BigDecimal portfolioValue = positionsRepo.findCurrentPortfolioValue(account.getAccountId());
            portfolioSnapshotRepo.insert(account.getAccountId(), portfolioValue, snapshotDate);
        }
    }

    public List<PortfolioSnapshotEntity> findAll() {
        return portfolioSnapshotRepo.findAll();
    }

    public List<PortfolioSnapshotEntity> findByAccountId(Integer accountId) {
        return portfolioSnapshotRepo.findByAccountId(accountId);
    }
}