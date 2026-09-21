package main.controllers;

import main.services.AdminService;
import main.entities.adminEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/admin")
public class adminController {
    private AdminService service;
    private PasswordEncoder passwordEncoder;
    public adminController(AdminService service, PasswordEncoder passwordEncoder){
        this.service = service;
        this.passwordEncoder = passwordEncoder;
    }
    @PostMapping("/create")
    public adminEntity createAdmin(@RequestBody adminEntity admin){
        String pass = passwordEncoder.encode(admin.getPassHash());
        admin.setPassHash(pass);
        return service.saveAdmin(admin);
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody adminEntity admin){
        return service.login(admin);
    }
}
