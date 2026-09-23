package main.controllers;

import main.services.PositionService;
import main.entities.PositionsEntity;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/positions")
public class PositionController {
    @Autowired
    private PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }
    @PostMapping("/create")
    public PositionsEntity savePosition(@RequestBody PositionsEntity position) {
        return positionService.savePosition(position);
    }
    @GetMapping("/{id}")
    public PositionsEntity getPositionById(@PathVariable Integer id) {
        return positionService.findById(id);
    }
    
}