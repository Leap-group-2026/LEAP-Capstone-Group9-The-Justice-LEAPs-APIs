package main.controllers;

import main.services.UserService;
import main.dto.request.UserRegistrationRequest;
import main.dto.request.VerifyPasswordReset;
import main.dto.request.LoginRequest;
import main.dto.request.UpdateNameRequest;
import main.dto.request.UpdateEmailRequest;
import main.dto.request.UpdateAddressRequest;
import main.dto.response.UserResponse;
import main.dto.response.UpdateNameResponse;
import main.dto.response.UpdateEmailResponse;
import main.dto.response.UpdateAddressResponse;
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

    @PatchMapping("/{id}/name")
    public ResponseEntity<UpdateNameResponse> updateName(
            @PathVariable Integer id,
            @RequestBody UpdateNameRequest request) {
        UpdateNameResponse response = service.updateUserName(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<UpdateEmailResponse> updateEmail(
            @PathVariable Integer id,
            @RequestBody UpdateEmailRequest request) {
        UpdateEmailResponse response = service.updateUserEmail(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/address")
    public ResponseEntity<UpdateAddressResponse> updateAddress(
            @PathVariable Integer id,
            @RequestBody UpdateAddressRequest request) {
        UpdateAddressResponse response = service.updateUserAddress(id, request);
        return ResponseEntity.ok(response);
    }
}