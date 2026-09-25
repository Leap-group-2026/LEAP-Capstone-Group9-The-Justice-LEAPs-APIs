package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserEntity {
    private Integer userId;
    @JsonProperty("name")
    private String name; 
    @JsonProperty("email")
    private String email;
    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth; 
    @JsonProperty("address")
    private String address; 
    @JsonProperty("ssn_hash")
    private String ssnHash; 
    @JsonProperty("pass_hash")
    private String passHash; 
    @JsonProperty("code")
    private String code; 

    public UserEntity() {
        
    }

    public Integer getUserId() {
        return userId;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSsnHash() {
        return ssnHash;
    }

    public void setSsnHash(String ssnHash) {
        this.ssnHash = ssnHash;
    }

    public String getPassHash() {
        return passHash;
    }

    public void setPassHash(String passHash) {
        this.passHash = passHash;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
