package main.controllers;

import main.services.AdminService;
import main.services.EmailService;
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
    private EmailService emailService;
    public adminController(AdminService service, PasswordEncoder passwordEncoder, EmailService emailService){
        this.service = service;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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

    @GetMapping("/email")
    public ResponseEntity<String> sendEmail(){
        emailService.sendEmail(
            "electrowiz67@gmail.com",
            "Jello",
            "Test email"
        );
        return ResponseEntity.ok("Email sent successfully");
    }
}
