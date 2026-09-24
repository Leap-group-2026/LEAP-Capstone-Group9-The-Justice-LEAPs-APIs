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
import java.time.LocalDateTime;

@Mapper
public interface PositionsRepo {
    @Select("SELECT * FROM positions WHERE position_id = #{positionId}")
    Optional<PositionsEntity> findById(Integer positionId);

    @Select("SELECT * FROM positions")
    List<PositionsEntity> findAll();

    @Select("SELECT * FROM positions WHERE account_id = #{accountId}")
    List<PositionsEntity> findByAccount(@Param("accountId") Integer accountId);

    @Insert("INSERT INTO positions (account_id, instrument_id, quantity, total_price, average_price, opened_at, closed_at) " +
            "VALUES (#{accountId}, #{instrumentId}, #{quantity}, #{totalPrice}, #{averagePrice}, #{openedAt}, #{closedAt})")
    void insert(@Param("accountId") Integer accountId,
                @Param("instrumentId") Integer instrumentId,
                @Param("quantity") Integer quantity,
                @Param("totalPrice") BigDecimal totalPrice,
                @Param("averagePrice") BigDecimal averagePrice,
                @Param("openedAt") LocalDateTime openedAt,
                @Param("closedAt") LocalDateTime closedAt);

    @Update("UPDATE positions SET account_id=#{accountId}, instrument_id=#{instrumentId}, " +
            "quantity=#{quantity}, total_price=#{totalPrice}, average_price=#{averagePrice}, " +
            "opened_at=#{openedAt}, closed_at=#{closedAt} WHERE position_id=#{positionId}")
    void update(@Param("positionId") Integer positionId,
                @Param("accountId") Integer accountId,
                @Param("instrumentId") Integer instrumentId,
                @Param("quantity") Integer quantity,
                @Param("totalPrice") BigDecimal totalPrice,
                @Param("averagePrice") BigDecimal averagePrice,
                @Param("openedAt") LocalDateTime openedAt,
                @Param("closedAt") LocalDateTime closedAt);

    @Delete("DELETE FROM positions WHERE position_id = #{positionId}")
    void delete(Integer positionId);
}
