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
import dto.response.NewsArticle;
import dto.request.ForeignExchangePriceRequest;
import dto.response.ValidationError;
import jakarta.validation.Valid;
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

    @Operation(summary = "Get instrument price by a ticker containing a slash",
        description = "Same as GET /instruments/{ticker}, for forex and crypto tickers such as USD/EUR or BTC/USD: "
            + "call /instruments/USD/EUR. An encoded slash (USD%2FEUR) is refused by the server.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Instrument found with current price"),
        @ApiResponse(responseCode = "404", description = "Instrument ticker not found",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @GetMapping("/{base}/{quote}")
    public InstrumentWithPrice getByPairTicker(@PathVariable String base, @PathVariable String quote) {
        return service.getTickerPrice(base + "/" + quote);
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

    @Operation(summary = "Get crypto bar closing prices",
               description = "Retrieves historical crypto bars from Alpaca and returns their closing prices.")
    @ApiResponse(responseCode = "200", description = "Crypto bar closing prices retrieved",
                 content = @Content(mediaType = "application/json",
                     array = @ArraySchema(schema = @Schema(implementation = BigDecimal.class))))
    @GetMapping("/crypto-bars")
    public List<BigDecimal> getCryptoBars(
        @Parameter(description = "Comma-separated crypto symbols", example = "BTC/USD,ETH/USD,SOL/USD")
        @RequestParam String symbols,
        @Parameter(description = "Bar timeframe, such as 1Day", example = "1Day")
        @RequestParam String timeframe,
        @Parameter(description = "Start date in YYYY-MM-DD format", example = "2026-09-01")
        @RequestParam String start,
        @Parameter(description = "End date in YYYY-MM-DD format", example = "2026-10-01")
        @RequestParam String end) {
        return service.getCryptoBarNumbers(symbols, timeframe, start, end);
    }

    @Operation(summary = "Get the latest market news")
    @ApiResponse(responseCode = "200", description = "Latest market news retrieved",
                 content = @Content(mediaType = "application/json"))
    @GetMapping("/news")
    public List<NewsArticle> getNews() {
        return service.getNews();
    }

    @Operation(summary = "Get a foreign exchange price from a JSON currency pair")
    @PostMapping("/forex/price")
    public InstrumentWithPrice getForexPrice(@Valid @RequestBody ForeignExchangePriceRequest request) {
        return service.getForexPrice(request.currencyPair());
    }
}


