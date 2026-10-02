package controllers;

import services.UserService;
import dto.request.UserRegistrationRequest;
import dto.request.VerifyPasswordReset;
import dto.request.LoginRequest;
import dto.request.UpdateNameRequest;
import dto.request.UpdateEmailRequest;
import dto.request.UpdateAddressRequest;
import dto.response.UserResponse;
import dto.response.UpdateNameResponse;
import dto.response.UpdateEmailResponse;
import dto.response.UpdateAddressResponse;
import dto.request.TransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dto.response.ValidationError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Users")  // description and display order: OpenApiConfig
@RestController 
@RequestMapping("/user")
public class UserController {
    private UserService service; 
    public UserController(UserService service){
        this.service = service; 
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

    @Operation(summary = "User login", description = "Checks a user's email and password. No session or token is issued yet.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials valid", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Login successful"))),
        @ApiResponse(responseCode = "400", description = "Unknown email (\"Email doesn't exist\") or wrong password (\"Wrong password\")",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Wrong password")))
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request){
        return service.login(request);
    }

    @Operation(summary = "Change a user's name")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Name updated"),
        @ApiResponse(responseCode = "400", description = "Blank name", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Name cannot be empty"))),
        @ApiResponse(responseCode = "404", description = "No user with this id", content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @PatchMapping("/{id}/name")
    public ResponseEntity<UpdateNameResponse> updateName(
            @PathVariable Integer id,
            @RequestBody UpdateNameRequest request) {
        UpdateNameResponse response = service.updateUserName(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change a user's email", description = "The new email must be well-formed and not used by another user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email updated"),
        @ApiResponse(responseCode = "400", description = "\"Email cannot be empty\", \"Invalid email format\" or \"Email already in use\"",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Email already in use"))),
        @ApiResponse(responseCode = "404", description = "No user with this id", content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @PatchMapping("/{id}/email")
    public ResponseEntity<UpdateEmailResponse> updateEmail(
            @PathVariable Integer id,
            @RequestBody UpdateEmailRequest request) {
        UpdateEmailResponse response = service.updateUserEmail(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change a user's address")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address updated"),
        @ApiResponse(responseCode = "400", description = "Blank address", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Address cannot be empty"))),
        @ApiResponse(responseCode = "404", description = "No user with this id", content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @PatchMapping("/{id}/address")
    public ResponseEntity<UpdateAddressResponse> updateAddress(
            @PathVariable Integer id,
            @RequestBody UpdateAddressRequest request) {
        UpdateAddressResponse response = service.updateUserAddress(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Currency exchange transaction")
    @PostMapping("/transactions/exchange")
    public ResponseEntity<String> currencyExchange(@Valid @RequestBody TransactionRequest transactionRequest){
        return service.currencyExchange(transactionRequest);
    }
}
