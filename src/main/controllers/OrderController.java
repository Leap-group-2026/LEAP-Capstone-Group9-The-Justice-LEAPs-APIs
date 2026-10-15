package controllers;

import org.springframework.web.bind.annotation.*;
import dto.response.OrderHistoryResponse;
import dto.response.HistoricalOrderResponse;
import java.util.List;

import jakarta.validation.Valid;
import dto.request.CreateOrderRequest;
import dto.response.OrderSubmissionResponse;
import services.OrderService;
import services.AccountService;
import config.AuthorizationUtil;
import entities.OrderEntity;
import dto.response.ValidationError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import repos.OrdersRepo;
import exception.ResourceNotFoundException;

@Tag(name = "Orders")  // description and display order: OpenApiConfig
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final AccountService accountService;
    private final AuthorizationUtil authorizationUtil;
    private final OrdersRepo ordersRepo;

    public OrderController(OrderService orderService, AccountService accountService, AuthorizationUtil authorizationUtil, OrdersRepo ordersRepo) {
        this.orderService = orderService;
        this.accountService = accountService;
        this.authorizationUtil = authorizationUtil;
        this.ordersRepo = ordersRepo;
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
        checkOwnsAccount(request.accountId());
        return orderService.createOrder(request);
    }

    @Operation(summary = "Get order history for an account",
        description = "Retrieves the list of orders for the specified account.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order history retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @GetMapping("/account/{accountId}")
    public List<OrderHistoryResponse> getOrderHistory(@PathVariable Integer accountId) {
        var account = accountService.findById(accountId);
        if (account == null || account.getUserId() == null) {
            throw new ResourceNotFoundException("Account", accountId.toString());
        }
        authorizationUtil.checkAccountAccess(accountId, account.getUserId().getUserId());
        return orderService.getOrderHistory(accountId);
    }

    @Operation(summary = "Get order details",
        description = "Returns the details of a specific order by its order ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found", 
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @GetMapping("/{orderId}")
    public OrderHistoryResponse getByOrderId(@PathVariable Integer orderId)
    {
        authorizationUtil.checkAdminAccess();
        return orderService.getByOrderId(orderId);
    }

    @Operation(summary = "Cancel an order",
        description = "Sets a PENDING order's status to CANCELED and records a historical snapshot. "
            + "The order row is kept for the audit trail, not deleted. Orders that are already "
            + "FILLED, DECLINED, FAILED or CANCELED cannot be canceled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order canceled; returns the updated order"),
        @ApiResponse(responseCode = "400", description = "Order is not PENDING (fieldName \"status\")",
            content = @Content(schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @PostMapping("/{orderId}/cancel")
    public OrderHistoryResponse cancelOrder(@PathVariable Integer orderId) {
        ordersRepo.findById(orderId).ifPresent(order -> checkOwnsAccount(order.getAccountId().getAccountId()));
        return orderService.cancelOrder(orderId);
    }


    private void checkOwnsAccount(Integer accountId) {
        accountService.findIfPresent(accountId)
            .filter(account -> account.getUserId() != null)
            .ifPresent(account -> authorizationUtil.checkAccountAccess(accountId, account.getUserId().getUserId()));
    }
}
