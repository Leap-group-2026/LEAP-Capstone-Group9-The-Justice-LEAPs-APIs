package main.controllers;

import main.services.PositionService;
import main.entities.PositionsEntity;
import main.entities.AccountsEntity;
import main.entities.InstrumentEntity;
import main.dto.request.CreatePositionRequest;
import main.dto.response.ValidationError;
import jakarta.validation.Valid;

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

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
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
    @PostMapping("/create")
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
    @Operation(summary = "Get a position",
        description = "Returns a position by id. Known issue: account, instrument and their ids come back null, "
            + "and an unknown id returns 200 with an empty body rather than 404.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The position, or an empty body if the id doesn't exist")
    })
    @GetMapping("/{id}")
    public PositionsEntity getPositionById(@PathVariable Integer id) {
        return positionService.findById(id);
    }
    
}