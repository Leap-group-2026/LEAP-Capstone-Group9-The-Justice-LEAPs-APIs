package services;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import repos.AdminRepo;
import repos.UserRepo;
import dto.response.OrderAdminResponse;
import dto.response.AdminLoginResponse;
import dto.response.TopBoughtStockResponse;
import dto.response.TransactionHistoryResponse;
import dto.response.AdminUserLookupResponse;
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
    private WarehouseAnalyticsService warehouseAnalyticsService;
    private SessionService sessionService;

    private static final Map<String, String> TOKEN_ROLES = Map.of(
        "SUPER ADMIN", "superadmin",
        "ADMIN", "admin",
        "REPORTER/ANALYST", "analyst"
    );

    public AdminService(
        AdminRepo repo,
        UserRepo userRepo,
        PasswordEncoder passwordEncoder,
        WarehouseAnalyticsService warehouseAnalyticsService
    , SessionService sessionService){
        this.repo = repo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.warehouseAnalyticsService = warehouseAnalyticsService;
        this.sessionService = sessionService;
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
        SessionService.Grant session = sessionService.create(role, admin.getAdminId());
        return new AdminLoginResponse(admin.getAdminId(), role, session.sid(), session.expiresAt().toString());
    }
    public List<OrderAdminResponse> getAllOrders() {
        return repo.getAllOrders();
    }

    public List<OrderAdminResponse> getOrdersByAccountId(Integer accountId) {
        return repo.getOrdersByAccountId(accountId);
    }

    public List<TransactionHistoryResponse> getAllTransactions() {
        return repo.getAllTransactions();
    }

    public List<TransactionHistoryResponse> getTransactionsByAccountId(Integer accountId) {
        return repo.getTransactionsByAccountId(accountId);
    }

    public List<UserEntity> getAllUsers() {
        return userRepo.findAll();
    }

    public List<TopBoughtStockResponse> getTopBoughtStocks() {
        return warehouseAnalyticsService.getTopBoughtStocks();
    }

    public AdminUserLookupResponse findUserByEmail(String email, AccountService accountService) {
        UserEntity user = userRepo.findByEmail(email.trim())
            .orElseThrow(() -> new exception.ResourceNotFoundException("User", email));
        return new AdminUserLookupResponse(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getDateOfBirth(),
            user.getAddress(),
            accountService.getAccountsByUserID(user.getUserId())
        );
    }
}
