package main.controllers;

import main.services.AccountService;
import main.entities.AccountsEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    @Autowired 
    private AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    @PostMapping("/create")
    public AccountsEntity saveAccount(@RequestBody AccountsEntity account) {
        return accountService.saveAccount(account);
    }
    @GetMapping ("/{id}")
    public AccountsEntity getAccountById(@PathVariable Integer id) {
        return accountService.findById(id);
    }
}
