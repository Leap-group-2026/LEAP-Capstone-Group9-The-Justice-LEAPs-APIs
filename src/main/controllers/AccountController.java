package main.controllers;

import main.services.AccountService;
import main.entities.AccountsEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    @PostMapping("/close/{id}")
    public ResponseEntity<String> closeAccount(@PathVariable Integer id, @RequestBody AccountsEntity account) {
        try {
            String result = accountService.closeAccount(id, account.getUserId().getUserId());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
