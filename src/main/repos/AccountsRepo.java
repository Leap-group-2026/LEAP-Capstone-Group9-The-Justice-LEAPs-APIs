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
import main.entities.AccountsEntity;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface AccountsRepo {
    @Select("SELECT a.account_id, a.user_id, a.balance, a.portfolio_size, a.trade_type, a.created_at, a.account_active, " +
            "u.user_id, u.name, u.email, u.date_of_birth, u.address, u.ssn_hash, u.pass_hash " +
            "FROM accounts a LEFT JOIN user_info u ON a.user_id = u.user_id " +
            "WHERE a.account_id = #{accountId} AND a.account_active = true")
    @Results({
        @Result(column = "account_id", property = "accountId", id = true),
        @Result(column = "balance", property = "balance"),
        @Result(column = "portfolio_size", property = "portfolioSize"),
        @Result(column = "trade_type", property = "trade_type"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "account_active", property = "accountActive"),
        @Result(column = "user_id", property = "user.userId"),
        @Result(column = "name", property = "user.name"),
        @Result(column = "email", property = "user.email"),
        @Result(column = "date_of_birth", property = "user.dateOfBirth"),
        @Result(column = "address", property = "user.address"),
        @Result(column = "ssn_hash", property = "user.ssnHash"),
        @Result(column = "pass_hash", property = "user.passHash")
    })
    Optional<AccountsEntity> findById(Integer accountId);

    @Select("SELECT a.account_id, a.user_id, a.balance, a.portfolio_size, a.trade_type, a.created_at, a.account_active, " +
            "u.user_id, u.name, u.email, u.date_of_birth, u.address, u.ssn_hash, u.pass_hash " +
            "FROM accounts a LEFT JOIN user_info u ON a.user_id = u.user_id " +
            "WHERE a.account_active = true")
    @Results({
        @Result(column = "account_id", property = "accountId", id = true),
        @Result(column = "balance", property = "balance"),
        @Result(column = "portfolio_size", property = "portfolioSize"),
        @Result(column = "trade_type", property = "trade_type"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "account_active", property = "accountActive"),
        @Result(column = "user_id", property = "user.userId"),
        @Result(column = "name", property = "user.name"),
        @Result(column = "email", property = "user.email"),
        @Result(column = "date_of_birth", property = "user.dateOfBirth"),
        @Result(column = "address", property = "user.address"),
        @Result(column = "ssn_hash", property = "user.ssnHash"),
        @Result(column = "pass_hash", property = "user.passHash")
    })
    List<AccountsEntity> findAll();

    @Select("SELECT a.account_id, a.user_id, a.balance, a.portfolio_size, a.trade_type, a.created_at, a.account_active, " +
            "u.user_id, u.name, u.email, u.date_of_birth, u.address, u.ssn_hash, u.pass_hash " +
            "FROM accounts a LEFT JOIN user_info u ON a.user_id = u.user_id " +
            "WHERE a.user_id = #{userId} AND a.account_active = true")
    @Results({
        @Result(column = "account_id", property = "accountId", id = true),
        @Result(column = "balance", property = "balance"),
        @Result(column = "portfolio_size", property = "portfolioSize"),
        @Result(column = "trade_type", property = "trade_type"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "account_active", property = "accountActive"),
        @Result(column = "user_id", property = "user.userId"),
        @Result(column = "name", property = "user.name"),
        @Result(column = "email", property = "user.email"),
        @Result(column = "date_of_birth", property = "user.dateOfBirth"),
        @Result(column = "address", property = "user.address"),
        @Result(column = "ssn_hash", property = "user.ssnHash"),
        @Result(column = "pass_hash", property = "user.passHash")
    })
    List<AccountsEntity> findByUser(@Param("userId") Integer userId);

    @Insert("INSERT INTO accounts (user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (#{user.userId}, #{balance}, #{portfolioSize}, #{trade_type}, #{createdAt}, #{accountActive})")
    @Options(useGeneratedKeys = true, keyProperty = "accountId", keyColumn = "account_id")
    void insert(AccountsEntity account);

    @Update("UPDATE accounts SET user_id=#{userId}, balance=#{balance}, portfolio_size=#{portfolioSize}, " +
            "trade_type=#{trade_type}, account_active=#{accountActive} WHERE account_id=#{accountId}")
    void update(@Param("accountId") Integer accountId,
                @Param("userId") Integer userId,
                @Param("balance") BigDecimal balance,
                @Param("portfolioSize") String portfolioSize,
                @Param("trade_type") String trade_type,
                @Param("accountActive") Boolean accountActive);

    // Admin-only method: retrieve account including inactive ones
    @Select("SELECT a.account_id, a.user_id, a.balance, a.portfolio_size, a.trade_type, a.created_at, a.account_active, " +
            "u.user_id, u.name, u.email, u.date_of_birth, u.address, u.ssn_hash, u.pass_hash " +
            "FROM accounts a LEFT JOIN user_info u ON a.user_id = u.user_id " +
            "WHERE a.account_id = #{accountId}")
    @Results({
        @Result(column = "account_id", property = "accountId", id = true),
        @Result(column = "balance", property = "balance"),
        @Result(column = "portfolio_size", property = "portfolioSize"),
        @Result(column = "trade_type", property = "trade_type"),
        @Result(column = "created_at", property = "createdAt"),
        @Result(column = "account_active", property = "accountActive"),
        @Result(column = "user_id", property = "user.userId"),
        @Result(column = "name", property = "user.name"),
        @Result(column = "email", property = "user.email"),
        @Result(column = "date_of_birth", property = "user.dateOfBirth"),
        @Result(column = "address", property = "user.address"),
        @Result(column = "ssn_hash", property = "user.ssnHash"),
        @Result(column = "pass_hash", property = "user.passHash")
    })
    Optional<AccountsEntity> findByIdIncludingInactive(Integer accountId);

    @Delete("DELETE FROM accounts WHERE account_id = #{accountId}")
    void delete(Integer accountId);

    @Select("SELECT account_id, balance, account_active FROM accounts WHERE account_id = #{accountId} FOR UPDATE")
    Optional<AccountsEntity> findByIdForUpdate(Integer accountId);

    @Update("UPDATE accounts SET balance = #{balance} WHERE account_id = #{accountId}")
    void updateBalance(@Param("accountId") Integer accountId, @Param("balance") BigDecimal balance);
}



