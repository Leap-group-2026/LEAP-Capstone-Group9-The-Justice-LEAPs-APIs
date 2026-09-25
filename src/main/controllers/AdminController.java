package main.controllers;

import main.services.AdminService;
import main.services.EmailService;
import main.entities.AdminEntity;
import main.dto.request.AdminCreation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private AdminService service;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    public AdminController(AdminService service, PasswordEncoder passwordEncoder, EmailService emailService){
        this.service = service;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    @PostMapping("/create")
    public AdminEntity createAdmin(@RequestBody AdminCreation admin){
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setUsername(admin.getUsername());
        String pass = passwordEncoder.encode(admin.getPassword());
        adminEntity.setPassHash(pass);
        return service.saveAdmin(adminEntity);
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AdminCreation admin){
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setUsername(admin.getUsername());
        adminEntity.setPassHash(admin.getPassword());
        return service.login(adminEntity);
    }

}
