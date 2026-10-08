package controllers;

import services.UserService;
import services.AccountService;
import config.AuthorizationUtil;
import dto.request.UserRegistrationRequest;
import dto.request.VerifyPasswordReset;
import dto.request.LoginRequest;
import dto.request.UpdateUserRequest;
import dto.response.UserResponse;
import dto.response.UpdateUserResponse;
import dto.response.AccountResponse;
import dto.request.TransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import dto.response.ValidationError;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import dto.response.UserLoginResponse;

@Tag(name = "Users")  // description and display order: OpenApiConfig
@RestController 
@RequestMapping("/user")
public class UserController {
    private UserService service;
    private AccountService accountService;
    private AuthorizationUtil authorizationUtil;
    public UserController(UserService service, AccountService accountService, AuthorizationUtil authorizationUtil){
        this.service = service;
        this.accountService = accountService; 
        this.authorizationUtil = authorizationUtil;
    }

    @Operation(summary = "Register a user",
        description = "Creates a customer and sends a welcome email to their address. Email and SSN must be unique; the SSN "
            + "is stored only as a hash. Password rules: at least 12 characters, 2 uppercase, 2 special characters, no underscores.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Missing field (e.g. \"Email is Required\"), duplicate (\"Email already exists.\", "
            + "\"SSN already exists.\") or password rule broken (e.g. \"Password must be a minimum of 12 characters.\")",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Email already exists.")))
    })
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRegistrationRequest request){
        //return service.saveUser(user);
        UserResponse response = service.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Reset a password with a code",
        description = "Sets a new password if the code matches the one issued by /user/resetpassword. The new password must meet the registration rules.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Password reset successfully"))),
        @ApiResponse(responseCode = "400", description = "\"Incorrect reset code\", \"User not found\", or a password rule broken",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Incorrect reset code"))),
        @ApiResponse(responseCode = "500", description = "Known issue: the user has never been issued a reset code", content = @Content)
    })
    @PostMapping("/resetpassword/reset")
    public ResponseEntity<String> resetPassword(@RequestBody VerifyPasswordReset user){
        return service.resetPassword(user);
    }

    @Operation(summary = "Request a reset code",
        description = "Generates a 6-digit reset code for the user with this email and stores it. Only email is read from the body. "
            + "Known issue: the code is emailed to a fixed address, not to the user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Code generated", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Email sent successfully"))),
        @ApiResponse(responseCode = "400", description = "No user with this email", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "User not found")))
    })
    @PostMapping("/resetpassword")
    public ResponseEntity<String> emailResetPassword(@RequestBody UserResponse user){
        return service.emailResetPassword(user);
    }

    @Operation(summary = "User login",
        description = "Checks a client's email and password and returns their identity for the token service. "
            + "Spring issues no token: the auth service signs one with role \"client\".")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials valid; the body is the client's identity",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserLoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unknown email or wrong password (identical response for both)",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Invalid email or password")))
    })
    @SecurityRequirements  
    @PostMapping("/login")
    public UserLoginResponse login(@RequestBody LoginRequest request){
        return service.login(request);
    }


    @Operation(summary = "Currency exchange transaction")
    @PostMapping("/transactions/exchange")
    public ResponseEntity<String> currencyExchange(@Valid @RequestBody TransactionRequest transactionRequest){
        accountService.findIfPresent(transactionRequest.accountId())
            .filter(account -> account.getUserId() != null)
            .ifPresent(account -> authorizationUtil.checkAccountAccess(account.getAccountId(), account.getUserId().getUserId()));
        return service.currencyExchange(transactionRequest);
    }

    @Operation(summary = "Retrieve all accounts for the current user",
        description = "Returns a list of all active accounts owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user's accounts")
    @GetMapping("/accounts")
    public List<AccountResponse> getUserAccounts() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountService.getAccountsByUserID(Integer.parseInt(userId));
    }

    @Operation(summary = "Update a user's profile",
        description = "Updates one or more of the user's name, email, or address. At least one field must be provided. "
            + "Email must be well-formed and not used by another user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "400", description = "\"Email cannot be empty\", \"Invalid email format\", "
            + "\"Email already in use\", or \"Name cannot be empty\", \"Address cannot be empty\"",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Email already in use"))),
        @ApiResponse(responseCode = "404", description = "No user with this id", content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @PatchMapping
    public ResponseEntity<UpdateUserResponse> updateUser(
            @RequestBody UpdateUserRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UpdateUserResponse response = service.updateUser(Integer.parseInt(userId), request);
        return ResponseEntity.ok(response);
    }

}
