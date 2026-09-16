package controllers;

import services.AccountService;
import entities.accountsEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class accountController {
    @Autowired 
    private AccountService accountService;

    public accountController(AccountService accountService) {
        this.accountService = accountService;
    }
    @PostMapping("/create")
    public accountsEntity saveAccount(@RequestBody accountsEntity account) {
        return accountService.saveAccount(account);
    }
    @GetMapping ("/{id}")
    public accountsEntity getAccountById(@PathVariable Integer id) {
        return accountService.findById(id);
    }
}
