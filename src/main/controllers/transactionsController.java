package main.controllers;

import main.services.transactionsService;
import main.entities.transactionsEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/transactions")
public class transactionsController {
    private transactionsService service;
    public transactionsController(transactionsService service) {
        this.service = service;
    }
    
}
