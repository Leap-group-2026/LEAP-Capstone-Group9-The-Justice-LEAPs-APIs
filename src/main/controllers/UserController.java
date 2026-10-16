package controllers;

import services.UserService;
import services.AccountService;
import config.AuthorizationUtil;
import dto.request.UserRegistrationRequest;
import dto.request.EmailResetRequest;
import dto.request.PasswordResetRequest;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import config.OpenApiConfig;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import dto.response.UserLoginResponse;
import dto.response.UserInfoDisplayed;

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
            + "is stored only as a hash. SSN: 9 digits, as XXX-XX-XXXX or XXXXXXXXX (both count as the same SSN). Date of birth: "
            + "YYYY-MM-DD, at least 18 and at most 120 years ago. Email is stored lowercase. "
            + "Password rules: at least 12 characters, 2 uppercase, 2 special characters, no underscores.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Missing field (e.g. \"Email is Required\"), duplicate (\"Email already exists.\", "
            + "\"SSN already exists.\"), bad format (\"Invalid email format\", \"SSN must be 9 digits (XXX-XX-XXXX or XXXXXXXXX)\"), "
            + "age out of range (\"You must be at least 18 years old to register.\") or password rule broken (e.g. \"Password must be a minimum of 12 characters.\"). "
            + "A date that is not YYYY-MM-DD, or a field of the wrong type, gets a JSON ValidationError naming the field instead.",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Email already exists.")))
    })
    @SecurityRequirement(name = OpenApiConfig.INTERNAL_KEY_SCHEME)
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
    @SecurityRequirements
    @PostMapping("/resetpassword/reset")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest request){
        return service.resetPassword(request);
    }

    @Operation(summary = "Request a reset code",
        description = "Generates a 6-digit reset code for the user with this email and stores it. Only email is read from the body. "
            + "Known issue: the code is emailed to a fixed address, not to the user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Code generated", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Email sent successfully"))),
        @ApiResponse(responseCode = "400", description = "No user with this email", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "User not found")))
    })
    @SecurityRequirements
    @PostMapping("/resetpassword")
    public ResponseEntity<String> emailResetPassword(@RequestBody EmailResetRequest request){
        return service.emailResetPassword(request);
    }

    @Operation(summary = "User login",
        description = "Checks a client's email and password, starts a new session and returns the client's id, the session id "
            + "(sid) and when the session ends at the latest (expiresAt, 8 hours after login). Spring issues no token: the auth "
            + "service signs one with role \"client\" and this sid. Every login is its own session.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials valid; the body is the client's id and new session",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserLoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unknown email or wrong password (identical response for both)",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Invalid email or password")))
    })
    @SecurityRequirement(name = OpenApiConfig.INTERNAL_KEY_SCHEME)  
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

    @Operation(summary = "Retrieve the current user's profile")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the user's profile")
    @GetMapping
    public ResponseEntity<UserInfoDisplayed> getUsersInfoForDisplay(){
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.getUsersInfoForDisplay(userId);
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
