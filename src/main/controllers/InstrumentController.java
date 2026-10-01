package controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import services.InstrumentService;
import dto.InstrumentWithPrice;
import dto.response.ValidationError;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public InstrumentWithPrice getByTicker(@PathVariable String ticker) {
        return service.getTickerPrice(ticker);
    }

    @Operation(summary = "Retrieve all available instruments", 
               description = "Returns a list of all tradable instruments with their current prices")
    @ApiResponse(responseCode = "200", 
                 description = "Successfully retrieved all instruments",
                 content = @Content(mediaType = "application/json"))
    @GetMapping
    public List<InstrumentWithPrice> getAllInstruments(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return service.getAllInstruments(pageNum, pageSize);
    }
}


