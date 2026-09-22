package main.controllers;

import main.services.userService;
import main.dto.userRegistrationRequest;
import main.dto.userResponse;
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
        //return service.saveUser(user);
        userResponse response = service.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}