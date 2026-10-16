package repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import entities.SessionEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface SessionRepo {
    @Insert("INSERT INTO user_sessions (session_id, user_id, admin_id, role, created_at, last_activity_at, expires_at) " +
            "VALUES (#{sessionId}, #{userId}, #{adminId}, #{role}, #{createdAt}, #{lastActivityAt}, #{expiresAt})")
    void insert(SessionEntity session);

    @Select("SELECT session_id, user_id, admin_id, role, created_at, last_activity_at, expires_at, revoked_at, revoked_reason " +
            "FROM user_sessions WHERE session_id = #{sessionId}")
    Optional<SessionEntity> findById(UUID sessionId);

    @Update("UPDATE user_sessions SET last_activity_at = #{now} " +
            "WHERE session_id = #{sessionId} AND last_activity_at < #{staleBefore}")
    int touch(@Param("sessionId") UUID sessionId, @Param("now") Instant now, @Param("staleBefore") Instant staleBefore);

    @Update("UPDATE user_sessions SET revoked_at = #{now}, revoked_reason = #{reason} " +
            "WHERE session_id = #{sessionId} AND revoked_at IS NULL")
    int revoke(@Param("sessionId") UUID sessionId, @Param("now") Instant now, @Param("reason") String reason);

    @Update("UPDATE user_sessions SET revoked_at = #{now}, revoked_reason = #{reason} " +
            "WHERE user_id = #{userId} AND revoked_at IS NULL")
    int revokeAllForUser(@Param("userId") Integer userId, @Param("now") Instant now, @Param("reason") String reason);

    @Update("UPDATE user_sessions SET revoked_at = #{now}, revoked_reason = #{reason} " +
            "WHERE admin_id = #{adminId} AND revoked_at IS NULL")
    int revokeAllForAdmin(@Param("adminId") Integer adminId, @Param("now") Instant now, @Param("reason") String reason);
}
