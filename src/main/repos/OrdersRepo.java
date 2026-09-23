package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import main.entities.OrderEntity;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface OrdersRepo {
    @Select("SELECT * FROM orders WHERE order_id = #{orderId}")
    Optional<OrderEntity> findById(Integer orderId);

    @Select("SELECT * FROM orders")
    List<OrderEntity> findAll();

    @Insert("INSERT INTO orders (side, account_id, instrument_id, status, quantity, total_price, created_at, updated_at) " +
            "VALUES (#{side}, #{accountId}, #{instrumentId}, #{status}, #{quantity}, #{totalPrice}, #{createdAt}, #{updatedAt})")
    void insert(@Param("side") String side,
                @Param("accountId") Integer accountId,
                @Param("instrumentId") Integer instrumentId,
                @Param("status") String status,
                @Param("quantity") Integer quantity,
                @Param("totalPrice") BigDecimal totalPrice,
                @Param("createdAt") Object createdAt,
                @Param("updatedAt") Object updatedAt);

    @Update("UPDATE orders SET side=#{side}, account_id=#{accountId}, instrument_id=#{instrumentId}, " +
            "status=#{status}, quantity=#{quantity}, total_price=#{totalPrice}, updated_at=#{updatedAt} WHERE order_id=#{orderId}")
    void update(@Param("orderId") Integer orderId,
                @Param("side") String side,
                @Param("accountId") Integer accountId,
                @Param("instrumentId") Integer instrumentId,
                @Param("status") String status,
                @Param("quantity") Integer quantity,
                @Param("totalPrice") BigDecimal totalPrice,
                @Param("updatedAt") Object updatedAt);

    @Delete("DELETE FROM orders WHERE order_id = #{orderId}")
    void delete(Integer orderId);
}
