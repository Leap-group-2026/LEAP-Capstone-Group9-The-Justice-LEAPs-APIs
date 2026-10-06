package controllers;

import services.AdminService;
import services.EmailService;
import services.AccountService;
import services.OrderService;
import entities.AdminEntity;
import entities.UserEntity;
import dto.request.AdminCreation;
import dto.response.OrderAdminResponse;
import dto.response.AccountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import dto.response.AdminLoginResponse;

@Tag(name = "Admin")  // description and display order: OpenApiConfig
@RestController
@RequestMapping("/admin")
public class AdminController {
    private AdminService service;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private AccountService accountService;
    private OrderService orderService;
    public AdminController(AdminService service, PasswordEncoder passwordEncoder, EmailService emailService, AccountService accountService, OrderService orderService){
        this.service = service;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.accountService = accountService;
        this.orderService = orderService;
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
        adminEntity.setRole("placeholder");
        return service.saveAdmin(adminEntity);
    }
    @Operation(summary = "Admin login",
        description = "Checks an admin's email and password and returns their identity and token role for the token service. "
            + "Spring issues no token. Roles map as SUPER ADMIN -> superadmin, ADMIN -> admin, REPORTER/ANALYST -> analyst.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials valid; the body is the admin's identity and token role",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminLoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unknown email or wrong password (identical response for both)",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Invalid email or password"))),
        @ApiResponse(responseCode = "403", description = "Credentials valid but the admin's role is placeholder or empty",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "This admin has no role assigned")))
    })
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

    @Operation(summary = "Retrieve all users",
        description = "Returns a list of all users")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved users")
    @GetMapping("/users")
    public List<UserEntity> getAllUsers() {
        return service.getAllUsers();
    }
}
