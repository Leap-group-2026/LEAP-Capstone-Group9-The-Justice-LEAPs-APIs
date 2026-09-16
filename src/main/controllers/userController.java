package controllers;

import services.userService; 
import entities.userEntity; 
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/user")
public class userController {
    private userService service; 
    public userController(userService service){
        this.service = service; 
    }
    @PostMapping("/create")
    public userEntity createUser(@RequestBody userEntity user){
        return service.saveUser(user);
    }
}