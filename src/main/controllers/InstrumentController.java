package main.controllers;

import main.services.InstrumentService;
import main.entities.InstrumentEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class InstrumentController {
    private InstrumentService service;
    public InstrumentController(InstrumentService service){
        this.service = service;
    }
}
