package main.controllers;

import main.services.userService;
import main.dto.request.userRegistrationRequest;
import main.dto.request.LoginRequest;
import main.dto.response.userResponse;
import main.entities.userEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/user")
public class userController {
    private userService service; 
    public userController(userService service){
        this.service = service; 
    }

    @PostMapping("/create")
    public ResponseEntity<userResponse> createUser(@RequestBody userRegistrationRequest request){
        userResponse response = service.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/resetpassword")
    public ResponseEntity<String> resetPassword(@RequestBody userResponse user){
        return service.resetPassword(user);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request){

        return service.login(request);
    }
}