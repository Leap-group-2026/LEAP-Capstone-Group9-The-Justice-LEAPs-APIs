import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import main.Application;
import entities.UserEntity;
import repos.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Import;
import test.config.TestSecurityConfig;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = Application.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("UserController Password Reset Endpoints Tests")
public class UserPasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserEntity testUser;
    private String testEmail;
    private String testCode;

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = new UserEntity();
        testUser.setName("Test User");
        testEmail = "testuser@example.com";
        testUser.setEmail(testEmail);
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testUser.setAddress("123 Test Street");
        testUser.setSsnHash("test_ssn_hash");
        testUser.setPassHash("test_password_hash");
        testCode = "123456"; // Six-digit code
        testUser.setCode(testCode);
        userRepo.insert(testUser);
    }

    // ==================== Email Reset Password Tests ====================

    @Test
    @DisplayName("emailResetPassword - Failure: Should fail when user does not exist")
    void testEmailResetPasswordUserNotFound() throws Exception {
        // Arrange
        String requestBody = objectMapper.writeValueAsString(
            new java.util.HashMap<String, String>() {{
                put("email", "nonexistent@example.com");
            }}
        );

        // Act & Assert
        mockMvc.perform(post("/user/resetpassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ==================== Reset Password Tests ====================

    @Test
    @DisplayName("resetPassword - Success: Should reset password with correct code")
    void testResetPasswordSuccess() throws Exception {
        // Arrange
        // Password must be: 12+ chars, 2+ uppercase, 2+ special chars, no underscores
        String requestBody = objectMapper.writeValueAsString(
            new java.util.HashMap<String, String>() {{
                put("email", testEmail);
                put("code", testCode);
                put("password", "NewSecure@@Pass");
            }}
        );

        // Act & Assert
        mockMvc.perform(post("/user/resetpassword/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successfully"));
    }

    @Test
    @DisplayName("resetPassword - Failure: Should reject with incorrect code")
    void testResetPasswordIncorrectCode() throws Exception {
        // Arrange
        // Password must be: 12+ chars, 2+ uppercase, 2+ special chars, no underscores
        String requestBody = objectMapper.writeValueAsString(
            new java.util.HashMap<String, String>() {{
                put("email", testEmail);
                put("code", "999999");
                put("password", "NewSecure@@Pass");
            }}
        );

        // Act & Assert
        mockMvc.perform(post("/user/resetpassword/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Incorrect reset code"));
    }

    private UUID liveSession(int userId) {
        UUID sid = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        jdbcTemplate.update(
            "INSERT INTO user_sessions (session_id, user_id, role, created_at, last_activity_at, expires_at) VALUES (?, ?, 'client', ?, ?, ?)",
            sid, userId, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now.plus(8, ChronoUnit.HOURS)));
        return sid;
    }

    private String revokedReason(UUID sid) {
        return jdbcTemplate.queryForObject("SELECT revoked_reason FROM user_sessions WHERE session_id = ?", String.class, sid);
    }

    private int otherUser() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (930501, 'Other User', 'other.reset@example.com', DATE '1990-01-01', '1 Test St', 'ssn-930501', 'pass')");
        return 930501;
    }

    private String resetBody(String code) throws Exception {
        return objectMapper.writeValueAsString(new java.util.HashMap<String, String>() {{
            put("email", testEmail);
            put("code", code);
            put("password", "NewSecure@@Pass");
        }});
    }

    @Test
    @DisplayName("resetPassword - Success revokes every session of that user and no one else's")
    void testResetPasswordRevokesAllOfTheUsersSessions() throws Exception {
        UUID first = liveSession(testUser.getUserId());
        UUID second = liveSession(testUser.getUserId());
        UUID someoneElse = liveSession(otherUser());

        mockMvc.perform(post("/user/resetpassword/reset").contentType(MediaType.APPLICATION_JSON).content(resetBody(testCode)))
            .andExpect(status().isOk());

        assertEquals("password_reset", revokedReason(first));
        assertEquals("password_reset", revokedReason(second));
        assertNull(jdbcTemplate.queryForObject("SELECT revoked_at FROM user_sessions WHERE session_id = ?", Timestamp.class, someoneElse),
            "another user's session was revoked");
    }

    @Test
    @DisplayName("resetPassword - A wrong code changes nothing, sessions included")
    void testResetPasswordWithAWrongCodeKeepsSessions() throws Exception {
        UUID sid = liveSession(testUser.getUserId());

        mockMvc.perform(post("/user/resetpassword/reset").contentType(MediaType.APPLICATION_JSON).content(resetBody("999999")))
            .andExpect(status().isBadRequest());

        assertNull(jdbcTemplate.queryForObject("SELECT revoked_at FROM user_sessions WHERE session_id = ?", Timestamp.class, sid));
    }
}
