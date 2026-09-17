package main.controllers;

import main.services.PositionService;
import main.entities.positionsEntity;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/positions")
public class positionController {
    @Autowired
    private PositionService positionService;

    public positionController(PositionService positionService) {
        this.positionService = positionService;
    }
    @PostMapping("/create")
    public positionsEntity savePosition(@RequestBody positionsEntity position) {
        return positionService.savePosition(position);
    }
    @GetMapping("/{id}")
    public positionsEntity getPositionById(@PathVariable Integer id) {
        return positionService.findById(id);
    }
    
}