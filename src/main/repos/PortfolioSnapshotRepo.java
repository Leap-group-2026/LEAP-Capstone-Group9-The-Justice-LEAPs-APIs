package repos;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PortfolioSnapshotRepo {
    @Insert("INSERT INTO portfolio_snapshot (account_id, portfolio_value, snapshot_date) " +
            "VALUES (#{accountId}, #{portfolioValue}, #{snapshotDate})")
    void insert(@Param("accountId") Integer accountId,
                @Param("portfolioValue") BigDecimal portfolioValue,
                @Param("snapshotDate") LocalDate snapshotDate);
}