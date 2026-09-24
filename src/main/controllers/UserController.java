package main.controllers;

import main.services.UserService;
import main.dto.request.UserRegistrationRequest;
import main.dto.request.VerifyPasswordReset;
import main.dto.request.LoginRequest;
import main.dto.response.UserResponse;
import main.entities.UserEntity;
import main.dto.request.VerifyPasswordReset;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/user")
public class UserController {
    private UserService service; 
    public UserController(UserService service){
        this.service = service; 
    }

    @PostMapping("/create")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRegistrationRequest request){
        //return service.saveUser(user);
        UserResponse response = service.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/resetpassword/reset")
    public ResponseEntity<String> resetPassword(@RequestBody VerifyPasswordReset user){
        return service.resetPassword(user);
    }

    @PostMapping("/resetpassword")
    public ResponseEntity<String> emailResetPassword(@RequestBody UserResponse user){
        return service.emailResetPassword(user);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request){
        return service.login(request);
    }

    
    
}