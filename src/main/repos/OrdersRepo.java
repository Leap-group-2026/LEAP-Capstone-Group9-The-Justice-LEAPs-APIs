package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import main.entities.OrderEntity;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface OrdersRepo {
    @Select("SELECT * FROM orders WHERE order_id = #{orderId}")
    @Results({
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "side", property = "side"),
        @Result(column = "account_id", property = "accountIdValue"),
        @Result(column = "instrument_id", property = "instrumentIdValue"),
        @Result(column = "status", property = "status"),
        @Result(column = "quantity", property = "quantity"),
        @Result(column = "total_price", property = "totalPrice"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    Optional<OrderEntity> findById(Integer orderId);

    @Select("SELECT * FROM orders")
    @Results({
        @Result(column = "order_id", property = "orderId"),
        @Result(column = "side", property = "side"),
        @Result(column = "account_id", property = "accountIdValue"),
        @Result(column = "instrument_id", property = "instrumentIdValue"),
        @Result(column = "status", property = "status"),
        @Result(column = "quantity", property = "quantity"),
        @Result(column = "total_price", property = "totalPrice"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "updated_at", property = "updatedAt")
    })
    List<OrderEntity> findAll();

    @Insert("INSERT INTO orders (side, account_id, instrument_id, status, quantity, total_price, created_at, updated_at) " +
            "VALUES (#{side}, #{accountIdValue}, #{instrumentIdValue}, #{status}, #{quantity}, #{totalPrice}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId")
    void insert(OrderEntity order);

    @Update("UPDATE orders SET side=#{side}, account_id=#{accountIdValue}, instrument_id=#{instrumentIdValue}, " +
            "status=#{status}, quantity=#{quantity}, total_price=#{totalPrice}, updated_at=#{updatedAt} WHERE order_id=#{orderId}")
    void update(OrderEntity order);

    @Delete("DELETE FROM orders WHERE order_id = #{orderId}")
    void delete(Integer orderId);
}
