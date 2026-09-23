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

import main.controllers.AdminController;
import main.Application;
import main.entities.AdminEntity;
import main.repos.AdminRepo;

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
        testAdmin.setUsername("aryann");
        testAdmin.setPassHash(passwordEncoder.encode("secret"));
        testAdmin.setAdminId(1);
        adminRepo.insert(testAdmin);
        
        mockMvc.perform(post("/admin/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"aryann\",\"pass_hash\":\"secret\"}"))
            .andExpect(status().isOk());
    }
    
    @Test
    public void wrongUsername() throws Exception{
        mockMvc.perform(post("/admin/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"arya\",\"pass_hash\":\"gg\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test 
    public void wrongPassword() throws Exception{
        mockMvc.perform(post("/admin/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"aryan\",\"pass_hash\":\"gg\"}"))
            .andExpect(status().isBadRequest());
    }
}