package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import main.entities.AccountsEntity;
import main.entities.PositionsEntity;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface PositionsRepo {
    @Select("SELECT * FROM positions WHERE position_id = #{positionId}")
    Optional<PositionsEntity> findById(Integer positionId);

    @Select("SELECT * FROM positions")
    List<PositionsEntity> findAll();

    @Select("SELECT * FROM positions WHERE account_id = #{accountId}")
    List<PositionsEntity> findByAccount(@Param("accountId") Integer accountId);

    @Insert("INSERT INTO positions (account_id, instrument_id, quantity, average_cost, created_at) " +
            "VALUES (#{accountId}, #{instrumentId}, #{quantity}, #{averageCost}, #{createdAt})")
    void insert(@Param("accountId") Integer accountId,
                @Param("instrumentId") Integer instrumentId,
                @Param("quantity") Integer quantity,
                @Param("averageCost") BigDecimal averageCost,
                @Param("createdAt") Object createdAt);

    @Update("UPDATE positions SET account_id=#{accountId}, instrument_id=#{instrumentId}, " +
            "quantity=#{quantity}, average_cost=#{averageCost} WHERE position_id=#{positionId}")
    void update(@Param("positionId") Integer positionId,
                @Param("accountId") Integer accountId,
                @Param("instrumentId") Integer instrumentId,
                @Param("quantity") Integer quantity,
                @Param("averageCost") BigDecimal averageCost);

    @Delete("DELETE FROM positions WHERE position_id = #{positionId}")
    void delete(Integer positionId);
}
