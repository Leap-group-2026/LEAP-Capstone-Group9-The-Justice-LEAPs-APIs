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
import main.entities.UserEntity;
import main.repos.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

@SpringBootTest(classes = Application.class)
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
}
