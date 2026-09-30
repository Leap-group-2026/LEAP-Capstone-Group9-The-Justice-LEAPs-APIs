package main.controllers;

import java.util.List;
import main.services.AccountService;
import main.entities.AccountsEntity;
import main.entities.UserEntity;
import main.dto.request.CreateAccountRequest;
import main.dto.request.CloseAccountRequest;
import main.dto.response.AccountResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import main.dto.response.ValidationError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Accounts")  // description and display order: OpenApiConfig
@RestController
@RequestMapping("/accounts")
public class AccountController {
    @Autowired 
    private AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    @Operation(summary = "Open an account",
        description = "Creates an active account for an existing user and returns it with its generated accountId. "
            + "Only the owner's userId is needed, not the full user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account created; accountId is populated"),
        @ApiResponse(responseCode = "400", description = "A field is missing or invalid (\"Validation failed\", fieldName names it), "
            + "or portfolioSize is not LOW, BALANCED or HIGH",
            content = @Content(schema = @Schema(implementation = ValidationError.class),
                examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Validation failed\",\"timestamp\":\"2026-09-28T20:35:41.21\",\"fieldName\":\"userId\",\"rejectedValue\":null,\"fieldMessage\":\"must not be null\"}"))),
        @ApiResponse(responseCode = "500", description = "userId is not an existing user (database foreign key)", content = @Content)
    })
    @PostMapping("/create")
    public AccountsEntity saveAccount(@RequestBody @Valid CreateAccountRequest request) {
        UserEntity owner = new UserEntity();
        owner.setUserId(request.userId());

        AccountsEntity account = new AccountsEntity();
        account.setUserId(owner);
        account.setBalance(request.balance());
        account.setPortfolioSize(request.portfolioSize());
        account.setTradeType(request.tradeType());
        return accountService.saveAccount(account);
    }
    @Operation(summary = "Get an account",
        description = "Returns an active account with its owning user. A closed account returns exactly the same 404 "
            + "as an id that never existed. This is deliberate: the service can't yet verify who is asking, "
            + "so revealing that a closed account exists would leak information.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active account"),
        @ApiResponse(responseCode = "404", description = "No active account with this id (missing or closed)",
            content = @Content(schema = @Schema(implementation = ValidationError.class),
                examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Account with ID 34 not found\",\"timestamp\":\"2026-09-28T16:43:33.77\",\"fieldName\":\"Account\",\"rejectedValue\":\"34\",\"fieldMessage\":\"Resource not found\"}")))
    })
    @GetMapping ("/{id}")
    public AccountsEntity getAccountById(@PathVariable Integer id) {
        return accountService.findById(id);
    }
    @Operation(summary = "Close an account",
        description = "Soft-deletes the account (account_active = false). The body names the owner: {\"userId\": N}. "
            + "The balance must be exactly 0. Closing an account that is already closed is rejected "
            + "with an explicit message. Note: the owner check trusts the userId in the body until auth exists.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account closed",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Success"))),
        @ApiResponse(responseCode = "400", description = "Rejected. Plain-text bodies: \"Account is already closed\", "
            + "\"Not a valid user\" (no account with this id), \"You are unauthorized to close this account, it does not belong to you\", "
            + "\"In order to close an account your balance must be exactly $0, please sell your holdings\", "
            + "\"Account user information is missing\". A missing userId returns the JSON \"Validation failed\" error instead.",
            content = {
                @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Account is already closed")),
                @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))
            })
    })
    @PostMapping("/close/{id}")
    public ResponseEntity<String> closeAccount(@PathVariable Integer id, @RequestBody @Valid CloseAccountRequest request) {
        try {
            String result = accountService.closeAccount(id, request.userId());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Get all accounts for a user",
        description = "Returns all active accounts owned by the specified user.")
    @ApiResponse(responseCode = "200", description = "List of active accounts for the user")
    @GetMapping("/user/{userId}")
    public List<AccountResponse> getAccountsbyUserId(@PathVariable int userId)
    {
        return accountService.getAccountsByUserID(userId);
    }
}
