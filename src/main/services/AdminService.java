package main.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import main.repos.AdminRepo;
import main.entities.AdminEntity;

@Service
public class AdminService {
    private AdminRepo repo;
    private PasswordEncoder passwordEncoder;
    public AdminService(AdminRepo repo, PasswordEncoder passwordEncoder){
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminEntity saveAdmin(AdminEntity entity){
        if (repo.existsByUsername(entity.getUsername())){
            throw new IllegalArgumentException("Username already exists");
        }
        return repo.save(entity);
    }

    public ResponseEntity<String> login(AdminEntity entity){
        if (!repo.existsByUsername(entity.getUsername())){
            throw new IllegalArgumentException("Username doesn't exist");
        }
        AdminEntity user = repo.findByUsername(entity.getUsername()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean match = passwordEncoder.matches(entity.getPassHash(), user.getPassHash());
        if(!match){
            return ResponseEntity.badRequest().body("Wrong password");
        }
        else{
            return ResponseEntity.ok("Login successful");
        }
    }
}
