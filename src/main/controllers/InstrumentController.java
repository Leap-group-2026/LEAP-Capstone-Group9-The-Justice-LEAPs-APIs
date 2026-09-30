package main.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import main.services.InstrumentService;
import main.dto.InstrumentWithPrice;
import main.dto.response.ValidationError;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Instruments", description = "Tradable instruments (reference data)")
@RestController
@RequestMapping("/instruments")
public class InstrumentController {
    private InstrumentService service;
    public InstrumentController(InstrumentService service){
        this.service = service;
    }

    @Operation(summary = "Get instrument price by ticker",
        description = "Returns the current price and details of a specific instrument by its ticker symbol.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Instrument found with current price"),
        @ApiResponse(responseCode = "404", description = "Instrument ticker not found",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @GetMapping("/{ticker}")
    public InstrumentWithPrice getByTicker(@PathVariable String ticker)
    {
        return service.getTickerPrice(ticker);
    }
}


