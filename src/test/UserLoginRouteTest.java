import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import main.Application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Goes through the real endpoint against H2, so it sees the JSON body the auth service will receive
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@Transactional
public class UserLoginRouteTest {
    private static final int USER_ID = 9101;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void insertUser() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Login Test', 'login.test@example.com', DATE '1990-01-01', '1 Test St', 'SSN_HASH_9101', ?)",
            USER_ID, passwordEncoder.encode("CorrectPass@@12"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/user/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void correctCredentialsReturnOnlyTheId() throws Exception {
        login("login.test@example.com", "CorrectPass@@12")
            .andExpect(status().isOk())
            .andExpect(content().json("{\"id\":" + USER_ID + "}", true));
    }

    @Test
    void wrongPasswordGets401() throws Exception {
        login("login.test@example.com", "WrongPass@@1234")
            .andExpect(status().isUnauthorized())
            .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void unknownEmailGets401() throws Exception {
        login("nobody@example.com", "CorrectPass@@12")
            .andExpect(status().isUnauthorized())
            .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGetIdenticalResponses() throws Exception {
        String wrongPassword = login("login.test@example.com", "WrongPass@@1234")
            .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("nobody@example.com", "CorrectPass@@12")
            .andReturn().getResponse().getContentAsString();

        assertEquals(wrongPassword, unknownEmail);
    }
}
