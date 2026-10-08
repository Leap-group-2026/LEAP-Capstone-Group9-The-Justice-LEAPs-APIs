package services;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import repos.AdminRepo;
import repos.UserRepo;
import dto.response.OrderAdminResponse;
import dto.response.AdminLoginResponse;
import exception.InvalidCredentialsException;
import exception.AdminRoleNotAssignedException;
import entities.AdminEntity;
import entities.UserEntity;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {
    private AdminRepo repo;
    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;

    private static final Map<String, String> TOKEN_ROLES = Map.of(
        "SUPER ADMIN", "superadmin",
        "ADMIN", "admin",
        "REPORTER/ANALYST", "analyst"
    );

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

    public AdminLoginResponse login(String email, String password){
        AdminEntity admin = repo.findByEmail(email)
            .filter(a -> password != null && passwordEncoder.matches(password, a.getPassHash()))
            .orElseThrow(InvalidCredentialsException::new);

        String role = admin.getRole() == null ? null : TOKEN_ROLES.get(admin.getRole());
        if (role == null) {
            throw new AdminRoleNotAssignedException();
        }
        return new AdminLoginResponse(admin.getAdminId(), role);
    }
    public List<OrderAdminResponse> getAllOrders() {
        return repo.getAllOrders();
    }

    public List<UserEntity> getAllUsers() {
        return userRepo.findAll();
    }
}
