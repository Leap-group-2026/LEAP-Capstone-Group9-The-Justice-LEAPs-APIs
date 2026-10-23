package controllers;

import config.AuthorizationUtil;
import entities.PortfolioSnapshotEntity;
import services.AccountService;
import services.PortfolioSnapshotService;
import dto.response.ValidationError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Portfolio Snapshots")
@RestController
@RequestMapping("/portfolio-snapshots")
public class PortfolioSnapshotController {
    private final PortfolioSnapshotService portfolioSnapshotService;
    private final AccountService accountService;
    private final AuthorizationUtil authorizationUtil;

    public PortfolioSnapshotController(PortfolioSnapshotService portfolioSnapshotService,
                                       AccountService accountService,
                                       AuthorizationUtil authorizationUtil) {
        this.portfolioSnapshotService = portfolioSnapshotService;
        this.accountService = accountService;
        this.authorizationUtil = authorizationUtil;
    }

    @Operation(summary = "Get all portfolio snapshots",
        description = "Returns every portfolio snapshot for every account. Admin users only.")
    @ApiResponse(responseCode = "200", description = "All portfolio snapshots")
    @GetMapping
    public List<PortfolioSnapshotEntity> findAll() {
        authorizationUtil.checkAdminAccess();
        return portfolioSnapshotService.findAll();
    }

    @Operation(summary = "Get portfolio snapshots for an account",
        description = "Returns every snapshot for an account. The account owner or an admin may access it.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Portfolio snapshots for the account"),
        @ApiResponse(responseCode = "404", description = "Account not found or not owned by the authenticated user",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @GetMapping("/account/{accountId}")
    public List<PortfolioSnapshotEntity> findByAccountId(@PathVariable Integer accountId) {
        var account = accountService.findById(accountId);
        if (account.getUserId() == null || account.getUserId().getUserId() == null) {
            throw new exception.ResourceNotFoundException("Account", accountId.toString());
        }
        authorizationUtil.checkAccountAccess(accountId, account.getUserId().getUserId());
        return portfolioSnapshotService.findByAccountId(accountId);
    }
}