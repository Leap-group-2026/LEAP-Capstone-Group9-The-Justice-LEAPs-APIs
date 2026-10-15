package controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import services.TransactionsService;
import services.AccountService;
import config.AuthorizationUtil;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import dto.response.TransactionHistoryResponse;
import dto.request.FundsTransferRequest;
import dto.response.FundsTransferResponse;
import dto.response.ValidationError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;

@Tag(name = "Transactions")  
@RestController 
@RequestMapping("/transactions")
public class TransactionsController {
    private TransactionsService service;
    private AccountService accountService;
    private AuthorizationUtil authorizationUtil;
    
    public TransactionsController(TransactionsService service, AccountService accountService, AuthorizationUtil authorizationUtil) {
        this.service = service;
        this.accountService = accountService;
        this.authorizationUtil = authorizationUtil;
    }

    @Operation(summary = "Get transactions by account ID", description = "Retrieves the list of transactions for the specified account.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/account/{accountId}")
    public List<TransactionHistoryResponse> getTransactionsByAccountId(@PathVariable Integer accountId) {
        var account = accountService.findById(accountId);
        if (account == null || account.getUserId() == null) {
            throw new exception.ResourceNotFoundException("Account", accountId.toString());
        }
        authorizationUtil.checkAccountAccess(accountId, account.getUserId().getUserId());
        return service.getTransactionsByAccountId(accountId);
    }

    @Operation(summary = "Deposit funds",
        description = "Simulated deposit: moves the amount straight into one of the authenticated client's active accounts "
            + "and completes immediately. fundingMethod is optional and is not stored.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deposit recorded; returns the transaction and the new balance"),
        @ApiResponse(responseCode = "400", description = "amount or fundingMethod is missing or invalid (\"Validation failed\", fieldName names it)",
            content = @Content(schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(responseCode = "403", description = "The caller is not a client", content = @Content),
        @ApiResponse(responseCode = "404", description = "Account not found, closed, or not the caller's",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @PostMapping("/account/{accountId}/deposit")
    public FundsTransferResponse deposit(@PathVariable Integer accountId, @RequestBody @Valid FundsTransferRequest request) {
        return service.deposit(accountId, requireClientId(), request.amount());
    }

    @Operation(summary = "Withdraw funds",
        description = "Simulated withdrawal: moves the amount straight out of one of the authenticated client's active accounts "
            + "and completes immediately. fundingMethod is optional and is not stored.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Withdrawal recorded; returns the transaction and the new balance"),
        @ApiResponse(responseCode = "400", description = "amount or fundingMethod is missing or invalid, or amount is more than "
            + "the balance (fieldName \"balance\")",
            content = @Content(schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(responseCode = "403", description = "The caller is not a client", content = @Content),
        @ApiResponse(responseCode = "404", description = "Account not found, closed, or not the caller's",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @PostMapping("/account/{accountId}/withdraw")
    public FundsTransferResponse withdraw(@PathVariable Integer accountId, @RequestBody @Valid FundsTransferRequest request) {
        return service.withdraw(accountId, requireClientId(), request.amount());
    }

    private Integer requireClientId() {
        Integer clientId = authorizationUtil.getCurrentClientId();
        if (clientId == null) {
            throw new AccessDeniedException("Only a client can move funds in or out of their own account");
        }
        return clientId;
    }
}
