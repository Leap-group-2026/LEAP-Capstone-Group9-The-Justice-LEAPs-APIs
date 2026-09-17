package main.controllers;

import main.services.InstrumentService;
import main.entities.instrumentEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class instrumentController {
    private InstrumentService service;
    public instrumentController(InstrumentService service){
        this.service = service;
    }
}
