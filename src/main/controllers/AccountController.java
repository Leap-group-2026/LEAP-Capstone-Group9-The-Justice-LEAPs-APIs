package controllers;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import services.AccountService;
import config.AuthorizationUtil;
import entities.AccountsEntity;
import entities.UserEntity;
import dto.request.CreateAccountRequest;
import dto.response.AccountResponse;
import dto.response.OrderAdminResponse;
import dto.response.OrderAccountResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import dto.response.ValidationError;
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
    
    private AuthorizationUtil authorizationUtil;

    public AccountController(AccountService accountService, AuthorizationUtil authorizationUtil) {
        this.accountService = accountService;
        this.authorizationUtil = authorizationUtil;
    }
    @Operation(summary = "Open an account",
        description = "Creates an active account for the authenticated user and returns it with its generated accountId.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account created; accountId is populated"),
        @ApiResponse(responseCode = "400", description = "A field is missing or invalid (\"Validation failed\", fieldName names it), "
            + "or portfolioSize is not LOW, BALANCED or HIGH",
            content = @Content(schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping
    public AccountsEntity saveAccount(@RequestBody @Valid CreateAccountRequest request) {
        Integer clientId = authorizationUtil.getCurrentClientId();
        if (clientId == null) {
            throw new AccessDeniedException("Only a client can open an account for themselves");
        }
        UserEntity owner = new UserEntity();
        owner.setUserId(clientId);

        AccountsEntity account = new AccountsEntity();
        account.setUserId(owner);
        account.setBalance(request.balance());
        account.setPortfolioSize(request.portfolioSize());
        account.setTradeType(request.tradeType());
        return accountService.saveAccount(account);
    }
    
    @Operation(summary = "Close an account",
        description = "Soft-deletes the account (account_active = false) for the authenticated user. "
            + "The balance must be exactly 0. Closing an account that is already closed is rejected "
            + "with an explicit message.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account closed",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Success"))),
        @ApiResponse(responseCode = "400", description = "Rejected. Plain-text bodies: \"Account is already closed\", "
            + "\"Not a valid user\" (no account with this id), \"You are unauthorized to close this account, it does not belong to you\", "
            + "\"In order to close an account your balance must be exactly $0, please sell your holdings\", "
            + "\"Account user information is missing\".",
            content = {
                @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Account is already closed")),
                @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))
            })
    })
    @PatchMapping("/close/{id}")
    public ResponseEntity<String> closeAccount(@PathVariable Integer id) {
        try {
            String result;
            if (authorizationUtil.isAdmin()) {
                result = accountService.closeAccountAsAdmin(id);
            } else {
                Integer clientId = authorizationUtil.getCurrentClientId();
                if (clientId == null) {
                    throw new AccessDeniedException("Only the account's owner or an admin can close it");
                }
                result = accountService.closeAccount(id, clientId);
            }
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
        authorizationUtil.checkUserAccess(userId);
        return accountService.getAccountsByUserID(userId);
    }

    @Operation(summary = "Retrieve all orders from a specific account", 
           description = "Returns a list of all orders for a specified account")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all orders from account specified")
    @GetMapping("/orders/{accountId}")
    public List<OrderAccountResponse> getAllOrdersById(@PathVariable Integer accountId) {
        var account = accountService.findById(accountId);
        if (account == null || account.getUserId() == null) {
            throw new exception.ResourceNotFoundException("Account", accountId.toString());
        }
        authorizationUtil.checkAccountAccess(accountId, account.getUserId().getUserId());
        return accountService.getAllOrdersById(accountId);
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
        var account = accountService.findById(id);
        if (account == null || account.getUserId() == null) {
            throw new exception.ResourceNotFoundException("Account", id.toString());
        }
        authorizationUtil.checkAccountAccess(id, account.getUserId().getUserId());
        return account;
    }
}
