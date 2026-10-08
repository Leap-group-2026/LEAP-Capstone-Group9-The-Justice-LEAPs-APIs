package controllers;

import services.PositionService;
import services.AccountService;
import config.AuthorizationUtil;
import entities.PositionsEntity;
import entities.AccountsEntity;
import entities.InstrumentEntity;
import dto.request.CreatePositionRequest;
import dto.response.ValidationError;
import dto.response.PositionResponse;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Positions")  // description and display order: OpenApiConfig
@RestController
@RequestMapping("/positions")
public class PositionController {
    @Autowired
    private PositionService positionService;
    
    private AccountService accountService;
    private AuthorizationUtil authorizationUtil;

    public PositionController(PositionService positionService, AccountService accountService, AuthorizationUtil authorizationUtil) {
        this.positionService = positionService;
        this.accountService = accountService;
        this.authorizationUtil = authorizationUtil;
    }
    @Operation(summary = "Record a position",
        description = "Inserts a holding of instrumentId for accountId. Only the two ids are needed, not the full account "
            + "or instrument. openedAt defaults to now. The response echoes the request: positionId is not populated with the generated id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Position stored (positionId is null in the response)"),
        @ApiResponse(responseCode = "400", description = "A field is missing or invalid (\"Validation failed\", fieldName names it)",
            content = @Content(schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(responseCode = "500", description = "accountId or instrumentId doesn't exist (database foreign key)", content = @Content)
    })
    @PostMapping
    public PositionsEntity savePosition(@RequestBody @Valid CreatePositionRequest request) {
        AccountsEntity account = new AccountsEntity();
        account.setAccountId(request.accountId());
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setInstrumentId(request.instrumentId());

        PositionsEntity position = new PositionsEntity();
        position.setAccountId(account);
        position.setInstrumentId(instrument);
        position.setQuantity(request.quantity());
        position.setTotalPrice(request.totalPrice());
        position.setAveragePrice(request.averagePrice());
        position.setOpenedAt(request.openedAt());
        position.setClosedAt(request.closedAt());
        return positionService.savePosition(position);
    }

    @Operation(summary = "Get all open positions for a specific account",
        description = "Retrieves the open positions (closed_at not set) for the specified account, newest first, "
            + "with the instrument details of each. A closed account returns the same 404 as a missing one.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Open positions for the account; an empty list if it holds none"),
        @ApiResponse(responseCode = "404", description = "No active account with this id (missing or closed)",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @GetMapping("/account/{accountId}")
    public List<PositionResponse> findOpenPositionsByAccountId(@PathVariable Integer accountId) {
        var account = accountService.findById(accountId);
        if (account == null || account.getUserId() == null) {
            throw new exception.ResourceNotFoundException("Account", accountId.toString());
        }
        authorizationUtil.checkAccountAccess(accountId, account.getUserId().getUserId());
        return positionService.findOpenPositionsByAccountId(accountId);
    }
    
    @Operation(summary = "Get a position",
        description = "Returns a position by id, open or closed, with the ids of its account and instrument.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The position"),
        @ApiResponse(responseCode = "404", description = "No position with this id",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @GetMapping("/{id}")
    public PositionsEntity getPositionById(@PathVariable Integer id) {
        authorizationUtil.checkAdminAccess();
        return positionService.findById(id);
    }

}
