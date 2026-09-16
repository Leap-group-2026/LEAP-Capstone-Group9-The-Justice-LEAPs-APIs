package main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="user_info")
public class userEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;
    @JsonProperty("name")
    @Column(name = "name")
    private String name; 
    @JsonProperty("email")
    @Column(name = "email")
    private String email;
    @JsonProperty("date_of_birth")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth; 
    @JsonProperty("address")
    @Column(name = "address")
    private String address; 
    @JsonProperty("ssn_hash")
    @Column(name = "ssn_hash")
    private String ssnHash; 
    @JsonProperty("pass_hash")
    @Column(name = "pass_hash")
    private String passHash; 

    public userEntity() {
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

}
