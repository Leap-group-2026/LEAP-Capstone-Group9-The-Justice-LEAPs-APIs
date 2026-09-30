package main.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import main.repos.AdminRepo;
import main.entities.AdminEntity;
import java.time.LocalDateTime;

@Service
public class AdminService {
    private AdminRepo repo;
    private PasswordEncoder passwordEncoder;
    public AdminService(AdminRepo repo, PasswordEncoder passwordEncoder){
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminEntity saveAdmin(AdminEntity entity){
        if (repo.existsByEmail(entity.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }
        // The insert binds created_at explicitly, so a null here would bypass the column default
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
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
}
