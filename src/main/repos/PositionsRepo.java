package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import main.entities.AccountsEntity;
import main.entities.PositionsEntity;
import main.entities.InstrumentEntity;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface PositionsRepo {
    @Select("SELECT * FROM positions WHERE position_id = #{positionId}")
    Optional<PositionsEntity> findById(Integer positionId);

    @Select("SELECT * FROM positions")
    List<PositionsEntity> findAll();

    @Select("SELECT p.* FROM positions p WHERE p.account_id = #{accountId}")
    @Results({
        @Result(column = "position_id", property = "positionId"),
        @Result(column = "quantity", property = "quantity"),
        @Result(column = "instrument_id", property = "instrument", 
                one = @One(select = "main.repos.InstrumentRepo.findById")),
        @Result(column = "opened_at", property = "openedAt"),
        @Result(column = "closed_at", property = "closedAt"),
        @Result(column = "total_price", property = "totalPrice"),
        @Result(column = "average_price", property = "averagePrice")
    })
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
