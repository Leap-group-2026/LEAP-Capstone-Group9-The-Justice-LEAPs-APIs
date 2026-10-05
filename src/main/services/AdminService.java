package services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import repos.AdminRepo;
import repos.UserRepo;
import dto.response.OrderAdminResponse;
import entities.AdminEntity;
import entities.UserEntity;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {
    private AdminRepo repo;
    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    public AdminService(AdminRepo repo, UserRepo userRepo, PasswordEncoder passwordEncoder){
        this.repo = repo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminEntity saveAdmin(AdminEntity entity){
        String email = entity.getEmail();
        if (email == null || repo.existsByEmail(email)){
            throw new IllegalArgumentException("Email already exists");
        }
        // The insert binds created_at explicitly, so a null here would bypass the column default
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        switch(email.toLowerCase().charAt(0)) {
            case 's':
                entity.setRole("SUPER ADMIN");
                break;
            case 'a':
                entity.setRole("ADMIN");
                break;
            case 'r':
                entity.setRole("REPORTER/ANALYST");
                break;
        }
        repo.insert(entity);
        return entity;
    }

    public ResponseEntity<String> login(AdminEntity entity){
        if (!repo.existsByEmail(entity.getEmail())){
            throw new IllegalArgumentException("Email doesn't exist");
        }
        AdminEntity user = repo.findByEmail(entity.getEmail()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean match = passwordEncoder.matches(entity.getPassHash(), user.getPassHash());
        if(!match){
            return ResponseEntity.badRequest().body("Wrong password");
        }
        else{
            return ResponseEntity.ok("Login successful");
        }
    }
    public List<OrderAdminResponse> getAllOrders() {
        return repo.getAllOrders();
    }

    public List<UserEntity> getAllUsers() {
        return userRepo.findAll();
    }
}
