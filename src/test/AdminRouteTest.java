package test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import main.controllers.AdminController;
import main.Application;
import main.entities.AdminEntity;
import main.repos.AdminRepo;

import java.time.LocalDateTime;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@Transactional
public class AdminRouteTest {
    @Autowired 
    MockMvc mockMvc;
    @Autowired
    AdminRepo adminRepo;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test 
    public void loginSuccess() throws Exception {
        AdminEntity testAdmin = new AdminEntity();
        testAdmin.setEmail("aryann");
        testAdmin.setPassHash(passwordEncoder.encode("secret"));
        testAdmin.setCreatedAt(LocalDateTime.now());
        adminRepo.insert(testAdmin);
        
        mockMvc.perform(post("/admin/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"aryann\",\"password\":\"secret\"}"))
            .andExpect(status().isOk());
    }
    
    @Test
    public void wrongUsername() throws Exception{
        mockMvc.perform(post("/admin/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"arya\",\"password\":\"gg\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test 
    public void wrongPassword() throws Exception{
        mockMvc.perform(post("/admin/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"aryan\",\"password\":\"gg\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void createAdminNeverReturnsPasswordHash() throws Exception {
        mockMvc.perform(post("/admin/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"new.admin@example.com\",\"password\":\"secret\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("new.admin@example.com"))
            .andExpect(content().string(not(containsString("pass_hash"))))
            .andExpect(content().string(not(containsString("passHash"))))
            .andExpect(content().string(not(containsString("$2a$"))));
    }
}
