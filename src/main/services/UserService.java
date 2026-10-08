package services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import entities.AccountsEntity;
import dto.request.TransactionRequest;

import org.springframework.beans.factory.annotation.Autowired;

import repos.UserRepo;
import entities.UserEntity; 
import dto.request.UserRegistrationRequest;
import dto.request.LoginRequest;
import dto.request.VerifyPasswordReset;
import dto.request.UpdateUserRequest;
import dto.response.UserResponse;
import dto.response.UpdateUserResponse;
import repos.InstrumentRepo;
import repos.AccountsRepo;
import repos.PositionsRepo;
import repos.CurrentPriceRepo;
import exception.ResourceNotFoundException;
import dto.response.UserLoginResponse;
import exception.InvalidCredentialsException;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class UserService {
    private UserRepo repo; 
    private EmailService emailService;
    private CurrentPriceRepo currentPriceRepo;
    private PasswordEncoder passwordEncoder;
    private AccountsRepo accountsRepo;
    private PositionsRepo positionsRepo;
    private InstrumentRepo instrumentRepo;
    private static final Random random = new Random();
    
    @Autowired
    public UserService(UserRepo repo, EmailService emailService, PasswordEncoder passwordEncoder, 
                       AccountsRepo accountsRepo, CurrentPriceRepo currentPriceRepo, 
                       PositionsRepo positionsRepo, InstrumentRepo instrumentRepo){
        this.repo = repo; 
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.accountsRepo = accountsRepo;
        this.currentPriceRepo = currentPriceRepo;
        this.positionsRepo = positionsRepo;
        this.instrumentRepo = instrumentRepo;
    }

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

        if (emailService != null) {
            try {
                String subject = "Welcome to Ribbit!";
                String body = "Hello " + user.getName() + ",\n\n" +
                        "Welcome to Ribbit! Your account has been successfully created.\n\n" +
                        "You can now log in to your account and start trading.\n\n" +
                        "If you have any questions or need assistance, please don't hesitate to reach out.\n\n" +
                        "Best regards,\n" +
                        "The Ribbit Trading Team";
                emailService.sendEmail(user.getEmail(), subject, body);
            } catch (Exception e) {
                System.err.println("Warning: Failed to send welcome email for user " + user.getEmail() + ": " + e.getMessage());
            }
        }

        return new UserResponse(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getDateOfBirth(),
            user.getAddress()
        );
    }

    public UserLoginResponse login(LoginRequest request){
        UserEntity user = repo.findByEmail(request.getEmail())
            .filter(u -> request.getPassword() != null
                && passwordEncoder.matches(request.getPassword(), u.getPassHash()))
            .orElseThrow(InvalidCredentialsException::new);
        return new UserLoginResponse(user.getUserId());
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

    // Uses SHA-256 to hash
    private String generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing SSN", e);
        }
    }

    public ResponseEntity<String> emailResetPassword(UserResponse entity){
        String email = entity.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        int rand = 100000 + random.nextInt(900000);
        String code = Integer.toString(rand);
        
        UserEntity user = repo.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setCode(code);
        repo.update(user);
        
        if (emailService != null) {
            try {
                String emailBody = "Hello " + user.getName() + ",\n\n" +
                    "We received a request to reset your password. Please use the code below to proceed with resetting your password.\n\n" +
                    "Reset Code: " + code + "\n\n" +
                    "This code will expire in 15 minutes. If you did not request a password reset, please ignore this email.\n\n" +
                    "For security reasons, never share this code with anyone.\n\n" +
                    "Best regards,\n" +
                    "The Ribbit Trading Team";
                
                emailService.sendEmail(
                    user.getEmail(),
                    "Password Reset Request - Ribbit Trading",
                    emailBody
                );
            } catch (Exception e) {
                System.err.println("Warning: Failed to send reset email for user " + user.getEmail() + ": " + e.getMessage());
                throw new RuntimeException("Failed to send reset email. Please try again later.");
            }
        }
        return ResponseEntity.ok("Email sent successfully");
    }

    public ResponseEntity<String> resetPassword(VerifyPasswordReset user){
        String email = user.getEmail();
        UserEntity userInDb = repo.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if(userInDb.getCode() == null || !userInDb.getCode().equals(user.getCode())){
            return ResponseEntity.badRequest().body("Incorrect reset code");
        }
        else{
            validatePass(user.getPassword());
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            repo.updatePassword(userInDb.getUserId(), encodedPassword);
            return ResponseEntity.ok("Password reset successfully");
        }
    }

    public UpdateUserResponse updateUser(Integer userId, UpdateUserRequest request) {
        UserEntity user = repo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim().toLowerCase();
            
            if (!isValidEmail(newEmail)) {
                throw new IllegalArgumentException("Invalid email format");
            }

            if (repo.existsByEmail(newEmail) && !user.getEmail().equalsIgnoreCase(newEmail)) {
                throw new IllegalArgumentException("Email already in use");
            }

            user.setEmail(newEmail);
        }

        if (request.address() != null && !request.address().isBlank()) {
            user.setAddress(request.address().trim());
        }

        repo.update(user);

        return new UpdateUserResponse(user.getUserId(), user.getName(), user.getEmail(), user.getAddress());
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex) && email.length() <= 255;
    }
    @Transactional 
    public ResponseEntity<String> currencyExchange(TransactionRequest transactionRequest){
        if (transactionRequest.baseAndExchange() == null || transactionRequest.baseAndExchange().isBlank()) {
            throw new IllegalArgumentException("Base and exchange currency pair is required");
        }
        
        String baseAndExchange = transactionRequest.baseAndExchange();
        String[] parts = baseAndExchange.split("/");
        AccountsEntity account = accountsRepo.findById(transactionRequest.accountId()).orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (parts.length < 2) throw new IllegalArgumentException("Invalid exchange rate format");
        if (!parts[0].equals("USD")){
            baseAndExchange = parts[1] + "/" + parts[0];
            Integer instrumentId = instrumentRepo.findIdBySymbol(baseAndExchange);
            if (instrumentId != null) {
                var position = positionsRepo.findOpenForUpdate(transactionRequest.accountId(), instrumentId);
                if (position.isPresent()) {
                    if(position.get().getTotalPrice().compareTo(transactionRequest.amount()) < 0){
                        return ResponseEntity.badRequest().body("You do not have enough currency in this exchange rate to make this exchange");
                    }
                }
                else{
                    return ResponseEntity.badRequest().body("You do not currently own any currency in this exchange rate");
                }
            }
            else{
                return ResponseEntity.badRequest().body("Foreign exchange chosen is not available for trade");
            }
            
        }
        else{
            if (account.getBalance().compareTo(transactionRequest.amount()) < 0) {
                return ResponseEntity.badRequest().body("You do not have enough balance in your account to make this exchange");
            }
        }
        final String priceTicker = baseAndExchange;
        BigDecimal price = currentPriceRepo.findPriceByTicker(priceTicker)
            .orElseThrow(() -> new IllegalArgumentException("Price not found for " + priceTicker));
        
        if (!parts[0].equals("USD")) {
            price = BigDecimal.ONE.divide(price, 10, RoundingMode.HALF_UP);
            BigDecimal usd = account.getBalance().add(price.multiply(transactionRequest.amount()));
            
            Integer instrumentId = instrumentRepo.findIdBySymbol(baseAndExchange);
            var position = positionsRepo.findOpenForUpdate(transactionRequest.accountId(), instrumentId);
            BigDecimal newTotalPrice = position.get().getTotalPrice().subtract(transactionRequest.amount());
            positionsRepo.update(position.get().getPositionId(), account.getAccountId(), instrumentId, position.get().getQuantity(), newTotalPrice, position.get().getAveragePrice(), position.get().getOpenedAt(), position.get().getClosedAt());
            accountsRepo.update(account.getAccountId(), account.getOwnerUserId(), usd, account.getPortfolioSize().getValue(), account.getTradeType(), account.getAccountActive());
        } else {
            BigDecimal foreign = transactionRequest.amount().multiply(price);
            BigDecimal usd = account.getBalance().subtract(transactionRequest.amount());
            Integer instrumentId = instrumentRepo.findIdBySymbol(baseAndExchange);
            var position = positionsRepo.findOpenForUpdate(transactionRequest.accountId(), instrumentId);
            
            if (position.isPresent()) {
                BigDecimal newTotalPrice = position.get().getTotalPrice().add(foreign);
                positionsRepo.update(position.get().getPositionId(), account.getAccountId(), instrumentId, position.get().getQuantity(), newTotalPrice, position.get().getAveragePrice(), position.get().getOpenedAt(), position.get().getClosedAt());
            } else {
                positionsRepo.insert(
                    transactionRequest.accountId(),
                    instrumentId,
                    1, 
                    foreign, 
                    price, 
                    LocalDateTime.now(), 
                    null 
                );
            }
            
            accountsRepo.update(account.getAccountId(), account.getOwnerUserId(), usd, account.getPortfolioSize().getValue(), account.getTradeType(), account.getAccountActive());
        }
        
        return ResponseEntity.ok("Transaction processed successfully");
    }
}
