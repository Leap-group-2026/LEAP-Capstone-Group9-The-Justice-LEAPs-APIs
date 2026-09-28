package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import main.entities.AdminEntity;
import java.util.Optional;
import java.util.List;

@Mapper
public interface AdminRepo {
    @Select("SELECT * FROM admin WHERE admin_id = #{adminId}")
    Optional<AdminEntity> findById(Integer adminId);

    @Select("SELECT * FROM admin")
    List<AdminEntity> findAll();

    @Insert("INSERT INTO admin (email, pass_hash, created_at) " +
            "VALUES (#{email}, #{passHash}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "adminId")
    void insert(AdminEntity admin);

    @Update("UPDATE admin SET email=#{email}, pass_hash=#{passHash} WHERE admin_id=#{adminId}")
    void update(AdminEntity admin);

    @Delete("DELETE FROM admin WHERE admin_id = #{adminId}")
    void delete(Integer adminId);

    @Select("SELECT EXISTS(SELECT 1 FROM admin WHERE email = #{email})")
    boolean existsByEmail(String email);

    @Select("SELECT * FROM admin WHERE email = #{email}")
    Optional<AdminEntity> findByEmail(String email);
}