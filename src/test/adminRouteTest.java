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

import main.controllers.adminController;
import main.Application;
import main.entities.adminEntity;
import main.repos.AdminRepo;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@Transactional
public class adminRouteTest {
    @Autowired 
    MockMvc mockMvc;
    @Autowired
    AdminRepo adminRepo;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test 
    public void loginSuccess() throws Exception {
        adminEntity testAdmin = new adminEntity();
        testAdmin.setUsername("aryann");
        testAdmin.setPassHash(passwordEncoder.encode("secret"));
        adminRepo.save(testAdmin);
        
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