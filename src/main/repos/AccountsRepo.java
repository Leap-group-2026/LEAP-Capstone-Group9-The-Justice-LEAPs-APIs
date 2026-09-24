package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Options;
import main.entities.AccountsEntity;
import main.entities.UserEntity;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface AccountsRepo {
    @Select("SELECT * FROM accounts WHERE account_id = #{accountId}")
    Optional<AccountsEntity> findById(Integer accountId);

    @Select("SELECT * FROM accounts")
    List<AccountsEntity> findAll();

    @Select("SELECT * FROM accounts WHERE user_id = #{userId}")
    List<AccountsEntity> findByUser(@Param("userId") Integer userId);

    @Insert("INSERT INTO accounts (user_id, balance, portfolio_size, trade_type, created_at) " +
            "VALUES (#{userId.userId}, #{balance}, #{portfolioSize}, #{tradeType}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "accountId")
    void insert(AccountsEntity account);

    @Update("UPDATE accounts SET user_id=#{userId.userId}, balance=#{balance}, portfolio_size=#{portfolioSize}, " +
            "trade_type=#{tradeType} WHERE account_id=#{accountId}")
    void update(AccountsEntity account);

    @Delete("DELETE FROM accounts WHERE account_id = #{accountId}")
    void delete(Integer accountId);
}



