package main.controllers;

import main.services.AdminService;
import main.services.EmailService;
import main.entities.AdminEntity;
import main.dto.request.AdminCreation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag(name = "Admin")  // description and display order: OpenApiConfig
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
    @Operation(summary = "Create an admin",
        description = "Creates an administrator with a bcrypt-hashed password. The username must be unique "
            + "(it is stored in the admin.email column).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admin created"),
        @ApiResponse(responseCode = "400", description = "Username already taken",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Username already exists")))
    })
    @PostMapping("/create")
    public AdminEntity createAdmin(@RequestBody AdminCreation admin){
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setUsername(admin.getUsername());
        String pass = passwordEncoder.encode(admin.getPassword());
        adminEntity.setPassHash(pass);
        return service.saveAdmin(adminEntity);
    }
    @Operation(summary = "Admin login", description = "Checks an admin username and password. No session or token is issued yet.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials valid", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Login successful"))),
        @ApiResponse(responseCode = "400", description = "Unknown username (\"Username doesn't exist\") or wrong password (\"Wrong password\")",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Wrong password")))
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AdminCreation admin){
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setUsername(admin.getUsername());
        adminEntity.setPassHash(admin.getPassword());
        return service.login(adminEntity);
    }

}
