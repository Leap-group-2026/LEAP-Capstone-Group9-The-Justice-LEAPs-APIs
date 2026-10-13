package repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import entities.OrderEntity;
import dto.response.OrderHistoryResponse;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface OrdersRepo {
    @Select("SELECT * FROM orders WHERE order_id = #{orderId}")
    @Results(id = "orderResult", value = {
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "side", property = "side"),
        @Result(column = "account_id", property = "accountId.accountId"),
        @Result(column = "instrument_id", property = "instrumentId.instrumentId"),
        @Result(column = "status", property = "status"),
        @Result(column = "quantity", property = "quantity"),
        @Result(column = "total_price", property = "totalPrice"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    Optional<OrderEntity> findById(Integer orderId);

    @Select("SELECT * FROM orders")
    @ResultMap("orderResult")
    List<OrderEntity> findAll();


    @Select("SELECT o.* FROM orders o " +
            "JOIN accounts a ON o.account_id = a.account_id " +
            "WHERE a.user_id = #{userId} " +
            "ORDER BY o.created_at DESC, o.order_id DESC")
    @ResultMap("orderResult")
    List<OrderEntity> findByUser(@Param("userId") Integer userId);

    @Select("SELECT o.* FROM orders o " +
            "JOIN accounts a ON o.account_id = a.account_id " +
            "WHERE a.user_id = #{userId} AND o.status = 'CANCELED' " +
            "ORDER BY o.updated_at DESC, o.order_id DESC")
    @ResultMap("orderResult")
    List<OrderEntity> findCanceledByUser(@Param("userId") Integer userId);


    @Select("SELECT order_id FROM orders WHERE status = 'PENDING' " +
            "AND created_at <= LOCALTIMESTAMP - CAST(#{minAgeSeconds} AS BIGINT) * INTERVAL '1' SECOND " +
            "ORDER BY created_at ASC, order_id ASC")
    List<Integer> findPendingOrderIds(@Param("minAgeSeconds") long minAgeSeconds);

    @Select("SELECT * from orders WHERE order_id = #{orderId} FOR UPDATE")
    @ResultMap("orderResult")
    Optional<OrderEntity> findByIdForUpdate(Integer orderId);

    @Select("SELECT o.order_id, i.ticker, o.side, o.status, o.quantity, " +
            "CAST(o.total_price / o.quantity AS NUMERIC(18,4)) AS price_per_unit, " +
            "o.total_price, " +
            "CASE WHEN o.status = 'FILLED' THEN o.updated_at END AS executed_at " +
            "FROM orders o " +
            "JOIN instruments i ON o.instrument_id = i.instrument_id " +
            "WHERE o.account_id = #{accountId} " +
            "ORDER BY o.created_at DESC, o.order_id DESC")
    List<OrderHistoryResponse> findOrdersByAccountId(@Param("accountId") Integer accountId);

    @Select("SELECT * FROM orders WHERE account_id = #{accountId}")
    @ResultMap("orderResult")
    List<OrderEntity> findAllByAccountId(@Param("accountId") Integer accountId);

    @Select("SELECT COALESCE(SUM(total_price), 0) FROM orders " +
            "WHERE account_id = #{accountId} AND side = 'BUY' AND status = 'FILLED' " +
            "AND updated_at >= #{startInclusive} AND updated_at < #{endExclusive}")
    BigDecimal sumFilledBuys(@Param("accountId") Integer accountId,
                             @Param("startInclusive") LocalDateTime startInclusive,
                             @Param("endExclusive") LocalDateTime endExclusive);

    @Insert("INSERT INTO orders (side, account_id, instrument_id, status, quantity, total_price) " +
            "VALUES (#{side}, #{accountId.accountId}, #{instrumentId.instrumentId}, #{status}, #{quantity}, #{totalPrice})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId,createdAt,updatedAt", keyColumn = "order_id,created_at,updated_at")
    void insert(OrderEntity order);

    @Update("UPDATE orders SET side=#{side}, account_id=#{accountId.accountId}, instrument_id=#{instrumentId.instrumentId}, " +
            "status=#{status}, quantity=#{quantity}, total_price=#{totalPrice}, updated_at=#{updatedAt} WHERE order_id=#{orderId}")
    void update(OrderEntity order);

    @Update("UPDATE orders SET total_price = #{totalPrice}, status = #{status}, updated_at = #{updatedAt} WHERE order_id = #{orderId}")
    void updateExecutionOutcome(@Param("orderId") Integer orderId,
                                @Param("totalPrice") BigDecimal totalPrice,
                                @Param("status") String status,
                                @Param("updatedAt") LocalDateTime updatedAt);


    @Update("UPDATE orders SET status = 'CANCELED', updated_at = #{updatedAt} " +
            "WHERE order_id = #{orderId} and status = 'PENDING'")
    int cancelOrder(@Param("orderId") Integer orderId,
                     @Param("updatedAt") LocalDateTime updatedAt);
}
