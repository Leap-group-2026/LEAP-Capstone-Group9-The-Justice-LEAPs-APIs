package main.controllers;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import main.dto.request.CreateOrderRequest;
import main.dto.response.OrderSubmissionResponse;
import main.services.OrderService;
import main.dto.response.ValidationError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Orders")  // description and display order: OpenApiConfig
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Place an order",
        description = "Creates a PENDING order and its audit snapshot in one transaction. The price comes from current_prices; "
            + "an instrument with no current price is rejected, never priced at zero. BUY requires enough balance and "
            + "market hours (9:30-16:00 US Eastern). SELL requires an open position holding at least the quantity. "
            + "A closed account is treated as missing (404).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order accepted"),
        @ApiResponse(responseCode = "400", description = "Invalid order. fieldName identifies the rule: "
            + "side/accountId/instrumentId/quantity (missing field or quantity not positive, message \"Validation failed\"); "
            + "balance (insufficient funds); price (BUY on an instrument with no current price); "
            + "instrument (SELL on an instrument with no current price); holdings (SELL more than held); "
            + "market_hours (BUY outside market hours)",
            content = @Content(schema = @Schema(implementation = ValidationError.class),
                examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Invalid order\",\"timestamp\":\"2026-09-28T18:00:04.93\",\"fieldName\":\"balance\",\"rejectedValue\":null,\"fieldMessage\":\"Insufficient balance for order: balance=$87500.0000, required=$189500.0000\"}"))),
        @ApiResponse(responseCode = "404", description = "Account missing or closed, or instrument not found",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @PostMapping
    public OrderSubmissionResponse createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
