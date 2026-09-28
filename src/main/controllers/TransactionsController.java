package main.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;

import main.services.TransactionsService;
import main.entities.TransactionsEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transactions", description = "Deposits, withdrawals and trade settlements. No endpoints yet")
@RestController 
@RequestMapping("/transactions")
public class TransactionsController {
    private TransactionsService service;
    public TransactionsController(TransactionsService service) {
        this.service = service;
    }
    
}
