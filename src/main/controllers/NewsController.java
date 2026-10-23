package controllers;

import config.AuthorizationUtil;
import dto.response.AccountNewsResponse;
import dto.response.ValidationError;
import exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import services.AccountService;
import services.MarketauxNewsService;

@Tag(name = "Accounts")
@RestController
@RequestMapping("/news")
public class NewsController {
    private final AccountService accountService;
    private final AuthorizationUtil authorizationUtil;
    private final MarketauxNewsService newsService;

    public NewsController(AccountService accountService, AuthorizationUtil authorizationUtil,
                          MarketauxNewsService newsService) {
        this.accountService = accountService;
        this.authorizationUtil = authorizationUtil;
        this.newsService = newsService;
    }

    @Operation(summary = "Get news for an account's holdings",
        description = "Returns recent Marketaux articles about the instruments the account holds open positions in. "
            + "sentiment: positive, neutral, negative or all (default). limit: 1-100, default 20.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Articles for the account's holdings; empty when it holds nothing"),
        @ApiResponse(responseCode = "400", description = "sentiment is not positive, neutral, negative or all",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string",
                example = "sentiment must be positive, neutral, negative, or all"))),
        @ApiResponse(responseCode = "404", description = "No active account with this id that the caller can see",
            content = @Content(schema = @Schema(implementation = ValidationError.class)))
    })
    @GetMapping("/account/{accountId}")
    public AccountNewsResponse findAccountNews(
            @PathVariable Integer accountId,
            @RequestParam(required = false, defaultValue = "all") String sentiment,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        var account = accountService.findById(accountId);
        if (account == null || account.getUserId() == null) {
            throw new ResourceNotFoundException("Account", accountId.toString());
        }
        authorizationUtil.checkAccountAccess(accountId, account.getUserId().getUserId());
        return newsService.findNews(accountId, sentiment, limit);
    }
}