package main.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import main.repos.userRepo;
import main.entities.userEntity; 
import main.dto.request.userRegistrationRequest;
import main.dto.response.userResponse;

import java.security.MessageDigest;
import java.util.Base64;

@Service
public class userService {
    private userRepo repo; 
    private PasswordEncoder passwordEncoder; 

    public userService(userRepo repo, PasswordEncoder passwordEncoder){
        this.repo = repo; 
        this.passwordEncoder = passwordEncoder;
    }

    /* 
    public userEntity saveUser(userEntity entity){
        return repo.save(entity);
    }*/

    public userResponse registerUser(userRegistrationRequest request){
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

        userEntity user = new userEntity(); 

        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress().trim());
        user.setSsnHash(ssnHash);
        user.setPassHash(passwordEncoder.encode(request.getPassword()));

        userEntity savedUser = repo.save(user);

        return new userResponse(
            savedUser.getUserId(),
            savedUser.getName(),
            savedUser.getEmail(),
            savedUser.getDateOfBirth(),
            savedUser.getAddress()
        );
    }


    // Ensures all fields are filled. 
    public void validateRequired(userRegistrationRequest request){
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
}