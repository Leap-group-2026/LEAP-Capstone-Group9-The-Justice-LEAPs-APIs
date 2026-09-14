package main.controllers;

import main.services.AdminService;
import main.entities.adminEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class adminController {
    private AdminService service;
    public adminController(AdminService service){
        this.service = service;
    }
    @PostMapping("/create")
    public adminEntity createAdmin(@RequestBody adminEntity admin){
        return service.saveAdmin(admin);
    }
}
