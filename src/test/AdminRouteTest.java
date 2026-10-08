import org.junit.jupiter.api.Test;
import config.InternalApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.notNullValue;

import main.Application;
import entities.AdminEntity;
import repos.AdminRepo;
import org.springframework.context.annotation.Import;
import test.config.TestSecurityConfig;

import java.time.LocalDateTime;

@SpringBootTest(classes = Application.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
@Transactional
public class AdminRouteTest {

    // The auth service's key; these endpoints refuse requests without it
    @Value("${internal.api.key}")
    private String internalApiKey;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    AdminRepo adminRepo;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private int insertAdmin(String email, String password, String role) {
        AdminEntity admin = new AdminEntity();
        admin.setEmail(email);
        admin.setPassHash(passwordEncoder.encode(password));
        admin.setCreatedAt(LocalDateTime.now());
        admin.setRole(role);
        adminRepo.insert(admin);
        return jdbcTemplate.queryForObject("SELECT admin_id FROM admin WHERE email = ?", Integer.class, email);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/admin/login")
            .header(InternalApiKeyFilter.HEADER, internalApiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    public void loginReturnsIdAndMappedRole() throws Exception {
        int id = insertAdmin("aryann", "secret", "ADMIN");

        login("aryann", "secret")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.role").value("admin"))
            .andExpect(jsonPath("$.passHash").doesNotExist())
            .andExpect(jsonPath("$.pass_hash").doesNotExist())
            .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    public void superAdminAndReporterMapToTokenRoles() throws Exception {
        insertAdmin("super1", "secret", "SUPER ADMIN");
        insertAdmin("reporter1", "secret", "REPORTER/ANALYST");

        login("super1", "secret").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("superadmin"));
        login("reporter1", "secret").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("analyst"));
    }

    @Test
    public void wrongUsername() throws Exception {
        login("nobody", "secret")
            .andExpect(status().isUnauthorized())
            .andExpect(content().string("Invalid email or password"));
    }

    @Test
    public void wrongPassword() throws Exception {
        insertAdmin("aryann", "secret", "ADMIN");

        login("aryann", "wrong")
            .andExpect(status().isUnauthorized())
            .andExpect(content().string("Invalid email or password"));
    }

    @Test
    public void wrongPasswordAndUnknownEmailGetIdenticalResponses() throws Exception {
        insertAdmin("aryann", "secret", "ADMIN");

        String wrongPassword = login("aryann", "wrong")
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("nobody", "secret")
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        assertEquals(wrongPassword, unknownEmail);
    }

    @Test
    public void placeholderRoleGets403WithoutIdentity() throws Exception {
        insertAdmin("bob", "secret", "placeholder");

        login("bob", "secret")
            .andExpect(status().isForbidden())
            .andExpect(content().string("This admin has no role assigned"));
    }

    @Test
    public void nullRoleGets403WithoutIdentity() throws Exception {
        insertAdmin("norole", "secret", null);

        login("norole", "secret")
            .andExpect(status().isForbidden())
            .andExpect(content().string("This admin has no role assigned"));
    }

    @Test
    public void placeholderWithWrongPasswordGets401Not403() throws Exception {
        // A 403 would confirm the email exists to someone who doesn't know the password
        insertAdmin("bob", "secret", "placeholder");

        login("bob", "wrong").andExpect(status().isUnauthorized());
    }

    @Test
    public void createAdminSucceeds() throws Exception {
        // Failed on every call before created_at was defaulted in AdminService
        mockMvc.perform(post("/admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@example.com\",\"password\":\"secret\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"))
            .andExpect(jsonPath("$.created_at", notNullValue()));
    }
}
