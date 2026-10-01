package controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import services.TransactionsService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import dto.response.TransactionHistoryResponse;

@Tag(name = "Transactions")  
@RestController 
@RequestMapping("/transactions")
public class TransactionsController {
    private TransactionsService service;
    public TransactionsController(TransactionsService service) {
        this.service = service;
    }

    @Operation(summary = "Get transactions by account ID", description = "Retrieves the list of transactions for the specified account.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/account/{accountId}")
    public List<TransactionHistoryResponse> getTransactionsByAccountId(@PathVariable Integer accountId) {
        return service.getTransactionsByAccountId(accountId);
    }
    
}
