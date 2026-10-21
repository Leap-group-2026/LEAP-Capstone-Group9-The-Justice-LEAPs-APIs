package controllers;

import config.AuthorizationUtil;
import dto.response.AccountNewsResponse;
import exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import services.AccountService;
import services.MarketauxNewsService;

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