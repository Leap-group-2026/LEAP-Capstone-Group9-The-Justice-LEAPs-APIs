package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import main.entities.UserEntity;
import java.util.Optional;
import java.util.List;

@Mapper
public interface UserRepo {
    @Select("SELECT * FROM user_info WHERE user_id = #{userId}")
    Optional<UserEntity> findById(Integer userId);

    @Select("SELECT * FROM user_info")
    List<UserEntity> findAll();

    @Insert("INSERT INTO user_info (name, email, date_of_birth, address, ssn_hash, pass_hash, code) " +
            "VALUES (#{name}, #{email}, #{dateOfBirth}, #{address}, #{ssnHash}, #{passHash}, #{code})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    void insert(UserEntity user);

    @Update("UPDATE user_info SET name=#{name}, email=#{email}, date_of_birth=#{dateOfBirth}, " +
            "address=#{address}, ssn_hash=#{ssnHash}, pass_hash=#{passHash}, code=#{code} WHERE user_id=#{userId}")
    void update(UserEntity user);

    @Delete("DELETE FROM user_info WHERE user_id = #{userId}")
    void delete(Integer userId);

    @Select("SELECT EXISTS(SELECT 1 FROM user_info WHERE email = #{email})")
    boolean existsByEmail(String email);

    @Select("SELECT EXISTS(SELECT 1 FROM user_info WHERE ssn_hash = #{ssnHash})")
    boolean existsBySsnHash(String ssnHash);

    @Select("SELECT * FROM user_info WHERE email = #{email}")
    Optional<UserEntity> findByEmail(String email);

    @Update("UPDATE user_info SET pass_hash=#{passHash} WHERE user_id=#{userId}")
    void updatePassword(@Param("userId") Integer userId, @Param("passHash") String passHash);
}
