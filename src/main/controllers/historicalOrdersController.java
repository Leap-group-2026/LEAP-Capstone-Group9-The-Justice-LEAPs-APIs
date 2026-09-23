package main.controllers;

import main.services.HistoricalOrdersService;
import main.entities.HistoricalOrdersEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class HistoricalOrdersController {
    private HistoricalOrdersService service;
    public HistoricalOrdersController(HistoricalOrdersService service){
        this.service = service;
    }
}

