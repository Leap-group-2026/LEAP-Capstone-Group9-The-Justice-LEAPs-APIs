package controllers;

import services.AdminService;
import services.EmailService;
import services.AccountService;
import services.OrderService;
import services.SessionService;
import entities.AdminEntity;
import entities.UserEntity;
import dto.request.AdminCreation;
import dto.response.OrderAdminResponse;
import dto.response.AccountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import config.OpenApiConfig;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import dto.response.AdminLoginResponse;
import dto.response.TopBoughtStockResponse;
import dto.response.DailyMoneyMovedResponse;
import services.OrderAnalyticsService;
import dto.response.TransactionHistoryResponse;
import dto.response.AdminUserLookupResponse;

@Tag(name = "Admin")  // description and display order: OpenApiConfig
@RestController
@RequestMapping("/admin")
public class AdminController {
    private AdminService service;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private AccountService accountService;
    private OrderService orderService;
    private SessionService sessionService;
    private OrderAnalyticsService orderAnalyticsService;
    public AdminController(AdminService service, PasswordEncoder passwordEncoder, EmailService emailService, AccountService accountService, OrderService orderService, SessionService sessionService, OrderAnalyticsService orderAnalyticsService){
        this.service = service;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.accountService = accountService;
        this.orderService = orderService;
        this.sessionService = sessionService;
        this.orderAnalyticsService = orderAnalyticsService;
    }
    @Operation(summary = "Create an admin",
        description = "Creates an administrator with a bcrypt-hashed password. The username must be unique "
            + "(it is stored in the admin.email column).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admin created"),
        @ApiResponse(responseCode = "400", description = "Username already taken",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Username already exists")))
    })
    @PostMapping
    public AdminEntity createAdmin(@RequestBody AdminCreation admin){
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setEmail(admin.getEmail());
        String pass = passwordEncoder.encode(admin.getPassword());
        adminEntity.setPassHash(pass);
        adminEntity.setRole("ADMIN");
        return service.saveAdmin(adminEntity);
    }
    @Operation(summary = "Admin login",
        description = "Checks an admin's email and password, starts a new session and returns the admin's id, token role, "
            + "session id (sid) and when the session ends at the latest (expiresAt). Spring issues no token. "
            + "Roles map as SUPER ADMIN -> superadmin, ADMIN -> admin, REPORTER/ANALYST -> analyst.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials valid; the body is the admin's id, token role and new session",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminLoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unknown email or wrong password (identical response for both)",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Invalid email or password"))),
        @ApiResponse(responseCode = "403", description = "Credentials valid but the admin's role is placeholder or empty",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "This admin has no role assigned")))
    })
    @SecurityRequirement(name = OpenApiConfig.INTERNAL_KEY_SCHEME) 
    @PostMapping("/login")
    public AdminLoginResponse login(@RequestBody AdminCreation admin){
        return service.login(admin.getEmail(), admin.getPassword());
    }

    @Operation(summary = "Retrieve all orders across all users and accounts", 
           description = "Returns a list of all orders for admin monitoring and auditing")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all orders")
    @GetMapping("/orders")
    public List<OrderAdminResponse> gettAllOrders() {
        return service.getAllOrders();
    }

    @Operation(summary = "Retrieve orders for an account")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the account's orders")
    @GetMapping("/orders/account/{accountId}")
    public List<OrderAdminResponse> getAccountOrders(@PathVariable Integer accountId) {
        return service.getOrdersByAccountId(accountId);
    }

    @Operation(summary = "Retrieve all transactions across all accounts")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all transactions")
    @GetMapping("/transactions")
    public List<TransactionHistoryResponse> getAllTransactions() {
        return service.getAllTransactions();
    }

    @Operation(summary = "Retrieve transactions for an account")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the account's transactions")
    @GetMapping("/transactions/account/{accountId}")
    public List<TransactionHistoryResponse> getAccountTransactions(@PathVariable Integer accountId) {
        return service.getTransactionsByAccountId(accountId);
    }

    @Operation(summary = "Retrieve all accounts across all users",
        description = "Returns a list of all active accounts for admin monitoring and auditing")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all accounts")
    @GetMapping("/accounts")
    public List<AccountResponse> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @Operation(summary = "Retrieve all accounts for a specific user",
        description = "Returns a list of all active accounts owned by the specified user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user's accounts")
    @GetMapping("/accounts/{userId}")
    public List<AccountResponse> getUserAccounts(@PathVariable Integer userId) {
        return accountService.getAccountsByUserID(userId);
    }

    @Operation(summary = "Retrieve all orders for a specific user",
           description = "Returns a list of all orders placed by the specified user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user's orders")
    @GetMapping("/orders/{userId}")
    public List<OrderAdminResponse> getUserOrders(@PathVariable Integer userId) {
        return orderService.getOrdersByUserID(userId);
    }

    @Operation(summary = "Retrieve all cancelled orders for a specific user",
           description = "Returns a list of all cancelled orders placed by the specified user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user's cancelled orders")
    @GetMapping("/orders/{userId}/cancelled")
    public List<OrderAdminResponse> getUserCancelledOrders(@PathVariable Integer userId) {
        return orderService.getCancelledOrdersByUserID(userId);
    }

    @Operation(summary = "Log a client out everywhere",
        description = "Revokes every session of the client with this user_id, so all their tokens stop working on the next "
            + "request (401 session_revoked). Their refresh tokens fail at the next refresh. Answers 204 even if the client "
            + "had no live session.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Every session of the client is revoked"),
        @ApiResponse(responseCode = "403", description = "The caller is not an admin", content = @Content)
    })
    @PostMapping("/users/{id}/logout")
    public ResponseEntity<Void> forceLogout(@PathVariable Integer id) {
        sessionService.revokeAllForUser(id, "admin_force_logout");
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Retrieve all users",
        description = "Returns a list of all users")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved users")
    @GetMapping("/users")
    public List<UserEntity> getAllUsers() {
        return service.getAllUsers();
    }

    @Operation(summary = "Retrieve the top bought stocks from the data warehouse")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved top bought stocks")
    @GetMapping("/analytics/top-bought")
    public List<TopBoughtStockResponse> getTopBoughtStocks() {
        return service.getTopBoughtStocks();
    }

    @Operation(summary = "Money moved by filled orders per day",
        description = "Sums total_price of FILLED orders by fill day (updated_at, Eastern time) and side, over the last "
            + "`days` days including today. Days with no fills are omitted.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Daily totals, oldest first"),
        @ApiResponse(responseCode = "400", description = "days is not between 1 and 365",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "days must be between 1 and 365")))
    })
    @GetMapping("/analytics/money-moved")
    public List<DailyMoneyMovedResponse> getMoneyMoved(@RequestParam(defaultValue = "30") int days) {
        return orderAnalyticsService.moneyMoved(days);
    }

    @Operation(summary = "Find a user by email with their accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User and accounts retrieved"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/by-email")
    public AdminUserLookupResponse getUserByEmail(@RequestParam String email) {
        return service.findUserByEmail(email, accountService);
    }
}
