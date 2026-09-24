package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import main.entities.HistoricalOrdersEntity;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface HistoricalOrdersRepo {
    @Select("SELECT * FROM historical_orders WHERE historical_order_id = #{historicalOrderId}")
    Optional<HistoricalOrdersEntity> findById(Integer historicalOrderId);

    @Select("SELECT * FROM historical_orders")
    List<HistoricalOrdersEntity> findAll();

    @Insert("INSERT INTO historical_orders (order_id, account_id, order_information_json, created_at) " +
            "VALUES (#{orderId}, #{accountId}, #{orderInformationJson}, #{createdAt})")
    void insert(@Param("orderId") Integer orderId, 
                @Param("accountId") Integer accountId,
                @Param("orderInformationJson") String orderInformationJson,
                @Param("createdAt") LocalDateTime createdAt);

    @Update("UPDATE historical_orders SET order_id=#{orderId}, account_id=#{accountId}, " +
            "order_information_json=#{orderInformationJson}, created_at=#{createdAt} WHERE historical_order_id=#{historicalOrderId}")
    void update(@Param("historicalOrderId") Integer historicalOrderId,
                @Param("orderId") Integer orderId, 
                @Param("accountId") Integer accountId,
                @Param("orderInformationJson") String orderInformationJson,
                @Param("createdAt") LocalDateTime createdAt);

    @Delete("DELETE FROM historical_orders WHERE historical_order_id = #{historicalOrderId}")
    void delete(Integer historicalOrderId);
}