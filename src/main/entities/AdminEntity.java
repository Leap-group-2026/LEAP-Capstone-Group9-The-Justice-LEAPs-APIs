package main.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class AdminEntity {
    private Integer adminId;
    @JsonProperty("username")
    private String username;
    @JsonProperty("pass_hash")
    private String passHash;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Getters and Setters
    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassHash() {
        return passHash;
    }

    public void setPassHash(String passHash) {
        this.passHash = passHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
