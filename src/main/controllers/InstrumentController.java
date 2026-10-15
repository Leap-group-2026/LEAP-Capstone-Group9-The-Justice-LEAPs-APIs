package controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import services.InstrumentService;
import dto.InstrumentWithPrice;
import dto.response.ValidationError;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
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

    @Operation(summary = "Get stock bar closing prices",
               description = "Retrieves historical stock bars from Alpaca and returns their closing prices.")
    @ApiResponse(responseCode = "200", description = "Stock bar closing prices retrieved",
                 content = @Content(mediaType = "application/json",
                     array = @ArraySchema(schema = @Schema(implementation = BigDecimal.class))))
    @GetMapping("/bars")
    public List<BigDecimal> getStockBars(
        @Parameter(description = "Comma-separated stock symbols, such as AAPL", example = "AAPL")
        @RequestParam String symbols,
        @Parameter(description = "Bar timeframe, such as 1Day", example = "1Day")
        @RequestParam String timeframe,
        @Parameter(description = "Start date in YYYY-MM-DD format", example = "2026-09-01")
        @RequestParam String start,
        @Parameter(description = "End date in YYYY-MM-DD format", example = "2026-10-01")
        @RequestParam String end) {
        return service.getBarNumbers(symbols, timeframe, start, end);
    }
}


