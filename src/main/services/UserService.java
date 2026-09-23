package main.services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import main.repos.UserRepo;
import main.entities.UserEntity; 
import main.dto.request.UserRegistrationRequest;
import main.dto.response.UserResponse;
import main.services.EmailService;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;

@Service
public class UserService {
    private UserRepo repo; 
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private static final Random random = new Random();
    
    public UserService(UserRepo repo, PasswordEncoder passwordEncoder){
        this.repo = repo; 
        this.passwordEncoder = passwordEncoder;
        this.emailService = null;
    }
    
    @Autowired
    public UserService(UserRepo repo, EmailService emailService, PasswordEncoder passwordEncoder){
        this.repo = repo; 
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /* 
    public UserEntity saveUser(UserEntity entity){
        return repo.save(entity);
    }*/

    public UserResponse registerUser(UserRegistrationRequest request){
        validateRequired(request);
        
        String email = request.getEmail().trim().toLowerCase();

        if(repo.existsByEmail(email)){
            throw new IllegalArgumentException("Email already exists.");
        }

        validatePass(request.getPassword());

        String ssnHash = generateSHA256Hash(request.getSsn());

        if(repo.existsBySsnHash(ssnHash)){
            throw new IllegalArgumentException("SSN already exists.");
        }

        UserEntity user = new UserEntity(); 

        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress().trim());
        user.setSsnHash(ssnHash);
        user.setPassHash(passwordEncoder.encode(request.getPassword()));

        repo.insert(user);

        return new UserResponse(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getDateOfBirth(),
            user.getAddress()
        );
    }


    // Ensures all fields are filled. 
    public void validateRequired(UserRegistrationRequest request){
        if (request.getName() == null || request.getName().isBlank()){
            throw new IllegalArgumentException("Name is Required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is Required");
        }
        if (request.getDateOfBirth() == null){
            throw new IllegalArgumentException("Date of Birth is Required");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()){
            throw new IllegalArgumentException("Address is Required");
        }
        if (request.getSsn() == null || request.getSsn().isBlank()){
            throw new IllegalArgumentException("SSN is Required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()){
            throw new IllegalArgumentException("Password is Required");
        }
    }

    // Ensures password meets requirements
    public void validatePass(String password){
        if(password.length() < 12){
            throw new IllegalArgumentException("Password must be a minimum of 12 characters.");
        }

        long uppercase = password.chars().filter(Character::isUpperCase).count();

        if(uppercase < 2){
            throw new IllegalArgumentException("Password must contain at least 2 uppercase characters.");
        }

        long specialCharacterCount = password.chars().filter(c -> !Character.isLetterOrDigit(c)).count();

        if(specialCharacterCount < 2){
            throw new IllegalArgumentException("Password must contain at least 2 special characters.");
        }

        if(password.contains("_")){
            throw new IllegalArgumentException("Password cannot contain underscores.");
        }
    }

    // Generates deterministic SHA-256 hash for SSN (for duplicate checking)
    private String generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing SSN", e);
        }
    }

    public ResponseEntity<String> resetPassword(UserResponse entity){
        String email = entity.getEmail();
        int rand = 100000 + random.nextInt(900000);
        String code = Integer.toString(rand);
        
        UserEntity user = repo.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setCode(code);
        repo.update(user);
        
        if (emailService != null) {
            String emailBody = "Hello " + user.getName() + ",\n\n" +
                "We received a request to reset your password. Please use the code below to proceed with resetting your password.\n\n" +
                "Reset Code: " + code + "\n\n" +
                "This code will expire in 15 minutes. If you did not request a password reset, please ignore this email.\n\n" +
                "For security reasons, never share this code with anyone.\n\n" +
                "Best regards,\n" +
                "The Ribbit Trading Team";
            
            emailService.sendEmail(
                email,
                "Password Reset Request - Ribbit Trading",
                emailBody
            );
        }
        return ResponseEntity.ok("Email sent successfully");
    }
}