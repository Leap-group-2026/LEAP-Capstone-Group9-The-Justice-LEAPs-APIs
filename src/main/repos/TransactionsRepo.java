package repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import entities.TransactionsEntity;
import dto.response.TransactionHistoryResponse;
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

    @Insert("INSERT INTO transactions (amount, side, account_id, transaction_type, happened_at) " +
            "VALUES (#{amount}, #{side}, #{accountId}, #{transactionType}, #{happenedAt})")
    void insert(@Param("amount") BigDecimal amount,
                @Param("side") String side,
                @Param("accountId") Integer accountId,
                @Param("transactionType") String transactionType,
                @Param("happenedAt") LocalDateTime happenedAt);

    @Update("UPDATE transactions SET amount=#{amount}, side=#{side}, account_id=#{accountId}, " +
            "transaction_type=#{transactionType}, happened_at=#{happenedAt} WHERE transaction_id=#{transactionId}")
    void update(@Param("transactionId") Integer transactionId,
                @Param("amount") BigDecimal amount,
                @Param("side") String side,
                @Param("accountId") Integer accountId,
                @Param("transactionType") String transactionType,
                @Param("happenedAt") LocalDateTime happenedAt);
        
     @Select("SELECT t.transaction_id, t.transaction_type, t.amount, t.side, t.account_id, t.happened_at " +
             "FROM transactions t WHERE t.account_id = #{accountId} " +
             "ORDER BY t.happened_at DESC, t.transaction_id DESC")
     List<TransactionHistoryResponse> getTransactionsByAccountId(@Param("accountId") Integer accountId);
}
