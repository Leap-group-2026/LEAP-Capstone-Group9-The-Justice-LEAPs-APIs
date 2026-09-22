package main.dto.response;

import java.time.LocalDate;

public class userResponse {
    private Integer userId; 
    private String name; 
    private String email; 
    private LocalDate dateOfBirth;
    private String address; 

    public userResponse() {
    }
    
    public userResponse(Integer userId, String name, String email, LocalDate dateOfBirth, String address){
        this.userId = userId;
        this.name = name; 
        this.email = email;
        this.dateOfBirth = dateOfBirth; 
        this.address = address;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDateOfBirth(){
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth){
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
