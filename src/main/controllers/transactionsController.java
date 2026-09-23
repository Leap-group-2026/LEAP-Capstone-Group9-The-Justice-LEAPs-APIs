package main.controllers;

import main.services.TransactionsService;
import main.entities.TransactionsEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/transactions")
public class TransactionsController {
    private TransactionsService service;
    public TransactionsController(TransactionsService service) {
        this.service = service;
    }
    
}
