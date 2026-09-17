package main.controllers;

import main.services.historicalOrdersService;
import main.entities.historicalOrdersEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class historicalOrdersController {
    private historicalOrdersService service;
    public historicalOrdersController(historicalOrdersService service){
        this.service = service;
    }
}

