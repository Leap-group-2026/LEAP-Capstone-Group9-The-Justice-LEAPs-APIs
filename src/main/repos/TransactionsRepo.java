package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import main.entities.TransactionsEntity;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface TransactionsRepo {
    @Select("SELECT * FROM transactions WHERE transaction_id = #{transactionId}")
    Optional<TransactionsEntity> findById(Integer transactionId);

    @Select("SELECT * FROM transactions")
    List<TransactionsEntity> findAll();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE account_id = #{accountId} AND side = 'OUT' AND transaction_type = 'TRADE' " +
            "AND happened_at >= #{startInclusive} AND happened_at < #{endExclusive}")
    BigDecimal sumExecutedBuys(@Param("accountId") Integer accountId,
                               @Param("startInclusive") LocalDateTime startInclusive,
                               @Param("endExclusive") LocalDateTime endExclusive);

    @Insert("INSERT INTO transactions (amount, side, account_id, transaction_type, happened_at) " +
            "VALUES (#{amount}, #{side}, #{accountId}, #{transactionType}, #{happenedAt})")
    void insert(@Param("amount") BigDecimal amount,
                @Param("side") String side,
                @Param("accountId") Integer accountId,
                @Param("transactionType") String transactionType,
                @Param("happenedAt") Object happenedAt);

    @Update("UPDATE transactions SET amount=#{amount}, side=#{side}, account_id=#{accountId}, " +
            "transaction_type=#{transactionType}, happened_at=#{happenedAt} WHERE transaction_id=#{transactionId}")
    void update(@Param("transactionId") Integer transactionId,
                @Param("amount") BigDecimal amount,
                @Param("side") String side,
                @Param("accountId") Integer accountId,
                @Param("transactionType") String transactionType,
                @Param("happenedAt") Object happenedAt);

    @Delete("DELETE FROM transactions WHERE transaction_id = #{transactionId}")
    void delete(Integer transactionId);
}