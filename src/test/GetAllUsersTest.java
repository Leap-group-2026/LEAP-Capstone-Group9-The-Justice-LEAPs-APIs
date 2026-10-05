import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import main.Application;
import entities.UserEntity;
import services.AdminService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@Transactional
public class GetAllUsersTest {
    @Autowired 
    MockMvc mockMvc;
    
    @MockBean
    AdminService adminService;

    @Test 
    public void getAllUsersSuccess() throws Exception {
        UserEntity user1 = new UserEntity();
        user1.setUserId(1);
        user1.setName("John Doe");
        user1.setEmail("john@example.com");
        
        UserEntity user2 = new UserEntity();
        user2.setUserId(2);
        user2.setName("Jane Smith");
        user2.setEmail("jane@example.com");
        
        List<UserEntity> users = Arrays.asList(user1, user2);
        when(adminService.getAllUsers()).thenReturn(users);
        
        mockMvc.perform(get("/admin/users")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("John Doe"))
            .andExpect(jsonPath("$[1].name").value("Jane Smith"));
    }
    
    @Test
    public void getAllUsersEmpty() throws Exception {
        List<UserEntity> users = Arrays.asList();
        when(adminService.getAllUsers()).thenReturn(users);
        
        mockMvc.perform(get("/admin/users")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
