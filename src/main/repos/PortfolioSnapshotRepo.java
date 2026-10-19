package repos;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import entities.PortfolioSnapshotEntity;
import java.util.List;

@Mapper
public interface PortfolioSnapshotRepo {
    @Select("SELECT portfolio_snapshot_id, account_id, portfolio_value, snapshot_date " +
            "FROM portfolio_snapshot ORDER BY snapshot_date DESC, portfolio_snapshot_id DESC")
    List<PortfolioSnapshotEntity> findAll();

    @Select("SELECT portfolio_snapshot_id, account_id, portfolio_value, snapshot_date " +
            "FROM portfolio_snapshot WHERE account_id = #{accountId} " +
            "ORDER BY snapshot_date DESC, portfolio_snapshot_id DESC")
    List<PortfolioSnapshotEntity> findByAccountId(@Param("accountId") Integer accountId);

    @Insert("INSERT INTO portfolio_snapshot (account_id, portfolio_value, snapshot_date) " +
            "VALUES (#{accountId}, #{portfolioValue}, #{snapshotDate})")
    void insert(@Param("accountId") Integer accountId,
                @Param("portfolioValue") BigDecimal portfolioValue,
                @Param("snapshotDate") LocalDate snapshotDate);
}