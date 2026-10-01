package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;

import main.dto.response.OrderAdminResponse;
import main.entities.AdminEntity;
import main.entities.OrderEntity;
import java.util.Optional;
import java.util.List;
import main.dto.response.OrderAdminResponse;

// The Java field is "email" and the database column is admin.email
@Mapper
public interface AdminRepo {
    @Select("SELECT admin_id, email, pass_hash, created_at, role FROM admin WHERE admin_id = #{adminId}")
    Optional<AdminEntity> findById(Integer adminId);

    @Select("SELECT admin_id, email, pass_hash, created_at, role FROM admin")
    List<AdminEntity> findAll();

    @Insert("INSERT INTO admin (email, pass_hash, created_at, role) " +
            "VALUES (#{email}, #{passHash}, #{createdAt}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "adminId")
    void insert(AdminEntity admin);

    @Update("UPDATE admin SET email=#{email}, pass_hash=#{passHash}, role=#{role} WHERE admin_id=#{adminId}")
    void update(AdminEntity admin);

    @Delete("DELETE FROM admin WHERE admin_id = #{adminId}")
    void delete(Integer adminId);

    @Select("SELECT EXISTS(SELECT 1 FROM admin WHERE email = #{email})")
    boolean existsByEmail(String email);

    @Select("SELECT admin_id, email, pass_hash, created_at, role FROM admin WHERE email = #{email}")
    Optional<AdminEntity> findByEmail(String email);

    @Select("SELECT * FROM orders")
    List<OrderAdminResponse> getAllOrders();
}