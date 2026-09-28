package main.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;

import main.services.InstrumentService;
import main.entities.InstrumentEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Instruments", description = "Tradable instruments (reference data). No endpoints yet")
@RestController
@RequestMapping("/admin")
public class InstrumentController {
    private InstrumentService service;
    public InstrumentController(InstrumentService service){
        this.service = service;
    }
}
