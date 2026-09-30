package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import main.entities.HistoricalOrdersEntity;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface HistoricalOrdersRepo {
    @Select("SELECT * FROM historical_orders WHERE historical_order_id = #{historicalOrderId}")
    @Results(id = "historicalOrderResult", value = {
        @Result(column = "historical_order_id", property = "historicalOrderId", id = true),
        @Result(column = "order_id", property = "orderId.orderId"),
        @Result(column = "account_id", property = "account.accountId"),
        @Result(column = "order_information_json", property = "orderInformationJson"),
        @Result(column = "created_at", property = "createdAt")
    })
    Optional<HistoricalOrdersEntity> findById(Integer historicalOrderId);

    @Select("SELECT * FROM historical_orders")
    @ResultMap("historicalOrderResult")
    List<HistoricalOrdersEntity> findAll();

    @Select("SELECT * FROM historical_orders WHERE order_id = #{orderId} ORDER BY created_at ASC")
    @ResultMap("historicalOrderResult")
    List<HistoricalOrdersEntity> findByOrderId_OrderIdOrderByCreatedAtAsc(@Param("orderId") Integer orderId);

    @Insert("INSERT INTO historical_orders (order_id, account_id, order_information_json, created_at) " +
            "VALUES (#{orderId}, #{accountId}, #{orderInformationJson, typeHandler=main.config.JsonbStringTypeHandler}, #{createdAt})")
    void insert(@Param("orderId") Integer orderId, 
                @Param("accountId") Integer accountId,
                @Param("orderInformationJson") String orderInformationJson,
                @Param("createdAt") LocalDateTime createdAt);

}
