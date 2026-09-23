import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import main.services.UserService;
import main.repos.UserRepo;
import main.entities.UserEntity;
import main.dto.request.UserRegistrationRequest;
import main.dto.request.LoginRequest;
import main.dto.response.UserResponse;

import java.time.LocalDate;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("User Service Tests")
public class UserServiceTest {

    private UserService service;

    @Mock
    private UserRepo mockUserRepo;

    @Mock
    private PasswordEncoder mockPasswordEncoder;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserService(mockUserRepo, mockPasswordEncoder);
    }

    // Helper method to generate SHA-256 hash (matches service implementation)
    private String generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing SSN in test", e);
        }
    }

    // checks valid registration
    @Test
    @DisplayName("Valid registration creates user successfully")
    public void testValidRegistration() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);
        String ssnHash = generateSHA256Hash("123-45-6789");
        when(mockUserRepo.existsBySsnHash(ssnHash)).thenReturn(false);
        when(mockPasswordEncoder.encode("SecurePass@123#")).thenReturn("hashed_password");

        UserEntity savedUser = new UserEntity();
        savedUser.setUserId(1);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        savedUser.setAddress("123 Main St");
        savedUser.setSsnHash(ssnHash);
        savedUser.setPassHash("hashed_password");

        when(mockUserRepo.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        UserResponse response = service.registerUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getUserId());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        verify(mockUserRepo, times(1)).save(any(UserEntity.class));
    }

    // checks response
    @Test
    @DisplayName("Successful registration returns safe response without password/SSN")
    public void testSuccessfulRegistrationReturnsSafeResponse() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("Jane Smith");
        request.setEmail("jane@example.com");
        request.setDateOfBirth(LocalDate.of(1985, 5, 15));
        request.setAddress("456 Oak Ave");
        request.setSsn("987-65-4321");
        request.setPassword("AnotherSecure@Pass1!");

        when(mockUserRepo.existsByEmail("jane@example.com")).thenReturn(false);
        String ssnHash2 = generateSHA256Hash("987-65-4321");
        when(mockUserRepo.existsBySsnHash(ssnHash2)).thenReturn(false);
        when(mockPasswordEncoder.encode("AnotherSecure@Pass1!")).thenReturn("hashed_password_2");

        UserEntity savedUser = new UserEntity();
        savedUser.setUserId(2);
        savedUser.setName("Jane Smith");
        savedUser.setEmail("jane@example.com");
        savedUser.setDateOfBirth(LocalDate.of(1985, 5, 15));
        savedUser.setAddress("456 Oak Ave");
        savedUser.setSsnHash(ssnHash2);
        savedUser.setPassHash("hashed_password_2");

        when(mockUserRepo.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        UserResponse response = service.registerUser(request);

        // Assert
        assertNotNull(response);
        // Verify response contains only safe fields
        assertEquals(2, response.getUserId());
        assertEquals("Jane Smith", response.getName());
        assertEquals("jane@example.com", response.getEmail());
        assertEquals(LocalDate.of(1985, 5, 15), response.getDateOfBirth());
        assertEquals("456 Oak Ave", response.getAddress());
        // Verify sensitive data not exposed in response
        assertTrue(!response.toString().contains("hashed_ssn"));
        assertTrue(!response.toString().contains("hashed_password"));
    }

    // checks if fields are null or empty
    @Test
    @DisplayName("Registration fails when name is missing")
    public void testMissingName() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName(null);
        request.setEmail("test@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Name is Required", exception.getMessage());
    }

    @Test
    @DisplayName("Registration fails when name is empty")
    public void testEmptyName() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("   ");
        request.setEmail("test@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Name is Required", exception.getMessage());
    }

    @Test
    @DisplayName("Registration fails when email is missing")
    public void testMissingEmail() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail(null);
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Email is Required", exception.getMessage());
    }

    @Test
    @DisplayName("Registration fails when address is missing")
    public void testMissingAddress() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress(null);
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Address is Required", exception.getMessage());
    }

    @Test
    @DisplayName("Registration fails when SSN is missing")
    public void testMissingSSN() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn(null);
        request.setPassword("SecurePass@123#");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("SSN is Required", exception.getMessage());
    }

    @Test
    @DisplayName("Registration fails when password is missing")
    public void testMissingPassword() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Password is Required", exception.getMessage());
    }

    @Test
    @DisplayName("Registration fails when date of birth is missing")
    public void testMissingDateOfBirth() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(null);
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Date of Birth is Required", exception.getMessage());
    }

    // checks password requirements (i.e. > 12 characters, >= 2 special characters, no _)
    @Test
    @DisplayName("Password rejected when less than 12 characters")
    public void testPasswordTooShort() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("Short@1");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Password must be a minimum of 12 characters.", exception.getMessage());
    }

    @Test
    @DisplayName("Password rejected when less than 2 uppercase characters")
    public void testPasswordInsufficientUppercase() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("onlyoneup@1234");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Password must contain at least 2 uppercase characters.", exception.getMessage());
    }

    @Test
    @DisplayName("Password rejected when less than 2 special characters")
    public void testPasswordInsufficientSpecial() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("OnlyOneSpecial@");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Password must contain at least 2 special characters.", exception.getMessage());
    }

    @Test
    @DisplayName("Password rejected when contains underscore")
    public void testPasswordContainsUnderscore() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("ValidPass_@123!");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Password cannot contain underscores.", exception.getMessage());
    }

    @Test
    @DisplayName("Password accepted with exactly 2 uppercase and 2 special characters")
    public void testPasswordMeetsRequirements() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("GoodPass@123!");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);
        String ssnHashPass = generateSHA256Hash("123-45-6789");
        when(mockUserRepo.existsBySsnHash(ssnHashPass)).thenReturn(false);
        when(mockPasswordEncoder.encode("GoodPass@123!")).thenReturn("hashed_password");

        UserEntity savedUser = new UserEntity();
        savedUser.setUserId(1);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        savedUser.setAddress("123 Main St");
        savedUser.setSsnHash(ssnHashPass);
        savedUser.setPassHash("hashed_password");

        when(mockUserRepo.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        UserResponse response = service.registerUser(request);

        // Assert
        assertNotNull(response);
        verify(mockUserRepo, times(1)).save(any(UserEntity.class));
    }

    // checks when email or ssn already exists
    @Test
    @DisplayName("Registration fails when email already exists")
    public void testEmailAlreadyExists() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("existing@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        when(mockUserRepo.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("Email already exists.", exception.getMessage());
    }

    @Test
    @DisplayName("Registration fails when SSN already exists")
    public void testSSNAlreadyExists() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("existing-ssn");
        request.setPassword("SecurePass@123#");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);
        String existingSsnHash = generateSHA256Hash("existing-ssn");
        when(mockUserRepo.existsBySsnHash(existingSsnHash)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.registerUser(request);
        });
        assertEquals("SSN already exists.", exception.getMessage());
    }

    // checks if password and ssn are hashed
    @Test
    @DisplayName("SSN is hashed before storage")
    public void testSSNHashedBeforeStorage() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);
        String ssnHashTest = generateSHA256Hash("123-45-6789");
        when(mockUserRepo.existsBySsnHash(ssnHashTest)).thenReturn(false);
        when(mockPasswordEncoder.encode("SecurePass@123#")).thenReturn("hashed_password");

        UserEntity savedUser = new UserEntity();
        savedUser.setUserId(1);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        savedUser.setAddress("123 Main St");
        savedUser.setSsnHash(ssnHashTest);
        savedUser.setPassHash("hashed_password");

        when(mockUserRepo.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        service.registerUser(request);

        // Assert - Verify SSN was hashed using SHA-256 (deterministic)
        verify(mockUserRepo, times(1)).existsBySsnHash(ssnHashTest);
    }

    @Test
    @DisplayName("Password is hashed before storage")
    public void testPasswordHashedBeforeStorage() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);
        String ssnHashPwd = generateSHA256Hash("123-45-6789");
        when(mockUserRepo.existsBySsnHash(ssnHashPwd)).thenReturn(false);
        when(mockPasswordEncoder.encode("SecurePass@123#")).thenReturn("hashed_password");

        UserEntity savedUser = new UserEntity();
        savedUser.setUserId(1);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        savedUser.setAddress("123 Main St");
        savedUser.setSsnHash(ssnHashPwd);
        savedUser.setPassHash("hashed_password");

        when(mockUserRepo.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        service.registerUser(request);

        // Assert - Verify password encoder was called for password
        verify(mockPasswordEncoder, times(1)).encode("SecurePass@123#");
    }

    @Test
    @DisplayName("Verify email is case-insensitive and trimmed")
    public void testEmailNormalizedBeforeCheck() {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("John Doe");
        request.setEmail("  JOHN@EXAMPLE.COM  ");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setAddress("123 Main St");
        request.setSsn("123-45-6789");
        request.setPassword("SecurePass@123#");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(false);
        String ssnHashEmail = generateSHA256Hash("123-45-6789");
        when(mockUserRepo.existsBySsnHash(ssnHashEmail)).thenReturn(false);
        when(mockPasswordEncoder.encode("SecurePass@123#")).thenReturn("hashed_password");

        UserEntity savedUser = new UserEntity();
        savedUser.setUserId(1);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        savedUser.setAddress("123 Main St");
        savedUser.setSsnHash(ssnHashEmail);
        savedUser.setPassHash("hashed_password");

        when(mockUserRepo.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        UserResponse response = service.registerUser(request);

        // Assert
        assertNotNull(response);
        verify(mockUserRepo, times(1)).existsByEmail("john@example.com");
    }

    // ===== LOGIN TESTS =====

    @Test
    @DisplayName("Successful login with correct email and password")
    public void testLoginSuccessful() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("SecurePass@123#");

        UserEntity existingUser = new UserEntity();
        existingUser.setUserId(1);
        existingUser.setEmail("john@example.com");
        existingUser.setPassHash("hashed_password");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(true);
        when(mockUserRepo.findByEmail("john@example.com")).thenReturn(java.util.Optional.of(existingUser));
        when(mockPasswordEncoder.matches("SecurePass@123#", "hashed_password")).thenReturn(true);

        // Act
        org.springframework.http.ResponseEntity<String> response = service.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Login successful", response.getBody());
        verify(mockUserRepo, times(1)).existsByEmail("john@example.com");
        verify(mockUserRepo, times(1)).findByEmail("john@example.com");
    }

    @Test
    @DisplayName("Login fails when email doesn't exist")
    public void testLoginEmailNotFound() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("SecurePass@123#");

        when(mockUserRepo.existsByEmail("nonexistent@example.com")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.login(loginRequest);
        });
        assertEquals("Email doesn't exist", exception.getMessage());
        verify(mockUserRepo, times(1)).existsByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Login fails with incorrect password")
    public void testLoginWrongPassword() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("WrongPassword@123#");

        UserEntity existingUser = new UserEntity();
        existingUser.setUserId(1);
        existingUser.setEmail("john@example.com");
        existingUser.setPassHash("hashed_correct_password");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(true);
        when(mockUserRepo.findByEmail("john@example.com")).thenReturn(java.util.Optional.of(existingUser));
        when(mockPasswordEncoder.matches("WrongPassword@123#", "hashed_correct_password")).thenReturn(false);

        // Act
        org.springframework.http.ResponseEntity<String> response = service.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Wrong password", response.getBody());
        verify(mockPasswordEncoder, times(1)).matches("WrongPassword@123#", "hashed_correct_password");
    }

    @Test
    @DisplayName("Login fails when user not found in database")
    public void testLoginUserNotFoundInDB() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("SecurePass@123#");

        when(mockUserRepo.existsByEmail("john@example.com")).thenReturn(true);
        when(mockUserRepo.findByEmail("john@example.com")).thenReturn(java.util.Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            service.login(loginRequest);
        });
        assertEquals("User not found", exception.getMessage());
    }
}
