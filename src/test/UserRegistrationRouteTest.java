import config.InternalApiKeyFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import main.Application;
import services.EmailService;
import test.config.TestClockConfig;
import test.config.TestSecurityConfig;

import java.security.MessageDigest;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /user through the real endpoint against H2, so it sees exactly what the auth service passes back to the
// browser. Rows are read back with JdbcTemplate rather than through the service that wrote them.
// TestClockConfig fixes today at 2024-09-24, which the age limits below are counted from.
@SpringBootTest(classes = Application.class)
@Import({TestSecurityConfig.class, TestClockConfig.class})
@AutoConfigureMockMvc
@Transactional
public class UserRegistrationRouteTest {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    ObjectMapper objectMapper;

    // The real one would try to reach smtp.gmail.com
    @MockBean
    EmailService emailService;

    private static final int EXISTING_USER_ID = 9301;
    private static final String EXISTING_SSN_DIGITS = "111223333";

    @BeforeEach
    void insertExistingUser() throws Exception {
        // Mixed case on purpose: the duplicate check must not depend on how the first user typed their email
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Existing User', 'Taken@Example.com', DATE '1990-01-01', '1 Test St', ?, 'PASS_HASH_SENTINEL')",
            EXISTING_USER_ID, sha256(EXISTING_SSN_DIGITS));
    }

    private static String sha256(String input) throws Exception {
        return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(input.getBytes()));
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "New User");
        body.put("email", "new.user@example.com");
        body.put("dateOfBirth", "1990-01-01");
        body.put("address", "2 Test St");
        body.put("ssn", "123-45-6789");
        body.put("password", "SecurePass@@12AB");
        return body;
    }

    private Map<String, Object> with(String field, Object value) {
        Map<String, Object> body = validBody();
        body.put(field, value);
        return body;
    }

    private ResultActions register(Map<String, Object> body) throws Exception {
        return register(objectMapper.writeValueAsString(body));
    }

    private ResultActions register(String json) throws Exception {
        return mockMvc.perform(post("/user")
            .header(InternalApiKeyFilter.HEADER, internalApiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
    }

    private int usersWithEmail(String email) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_info WHERE LOWER(email) = LOWER(?)", Integer.class, email);
    }

    // ---- The valid case, so every rejection below is known to be caused by the one field it changes ----

    @Test
    void aValidBodyIsStoredWithLowercaseEmailAndDigitsOnlySsnHash() throws Exception {
        register(with("email", "  New.User@Example.COM ")).andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("new.user@example.com"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT email, ssn_hash, date_of_birth FROM user_info WHERE email = 'new.user@example.com'");
        assertEquals("new.user@example.com", row.get("EMAIL"));
        assertEquals(sha256("123456789"), row.get("SSN_HASH"));
        assertEquals(java.sql.Date.valueOf("1990-01-01"), row.get("DATE_OF_BIRTH"));

        Integer userId = jdbcTemplate.queryForObject(
            "SELECT user_id FROM user_info WHERE email = 'new.user@example.com'", Integer.class);
        Map<String, Object> account = jdbcTemplate.queryForMap(
            "SELECT balance, portfolio_size, trade_type, account_active, account_frozen " +
            "FROM accounts WHERE user_id = ?", userId);
        assertEquals(0, ((BigDecimal) account.get("BALANCE")).compareTo(BigDecimal.ZERO));
        assertEquals("BALANCED", account.get("PORTFOLIO_SIZE"));
        assertEquals("Passive", account.get("TRADE_TYPE"));
        assertEquals(true, account.get("ACCOUNT_ACTIVE"));
        assertEquals(false, account.get("ACCOUNT_FROZEN"));
    }

    @Test
    void anSsnWithoutDashesIsAccepted() throws Exception {
        register(with("ssn", "123456789")).andExpect(status().isCreated());
    }

    // ---- SSN ----

    @Test
    void ssnsThatAreNotNineDigitsGet400() throws Exception {
        for (String ssn : new String[] {"12345678", "1234567890", "12-345-6789", "123-45-678a", "abcdefghi", "123 45 6789"}) {
            register(with("ssn", ssn))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("SSN must be 9 digits (XXX-XX-XXXX or XXXXXXXXX)"));
        }
        assertEquals(0, usersWithEmail("new.user@example.com"));
    }

    @Test
    void theSameSsnWithDashesIsStillADuplicate() throws Exception {
        register(with("ssn", "111-22-3333"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("SSN already exists."));
        assertEquals(0, usersWithEmail("new.user@example.com"));
    }

    // ---- Email ----

    @Test
    void badEmailFormatsGet400() throws Exception {
        for (String email : new String[] {"no-at-sign.com", "jo@", "@example.com", "jo@example", "jo smith@example.com"}) {
            register(with("email", email))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid email format"));
        }
    }

    @Test
    void anEmailThatDiffersOnlyByCaseIsADuplicate() throws Exception {
        register(with("email", "taken@example.com"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Email already exists."));
        assertEquals(1, usersWithEmail("taken@example.com"));
    }

    // ---- Date of birth: format ----

    @Test
    void datesNotInIsoFormatGet400NamingTheField() throws Exception {
        for (String date : new String[] {"01/15/1990", "15-01-1990", "1990/01/15", "Jan 15 1990", "1990-13-01", "1990-02-30"}) {
            register(with("dateOfBirth", date))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldName").value("dateOfBirth"))
                .andExpect(jsonPath("$.fieldMessage").value("dateOfBirth must be a valid date in YYYY-MM-DD format"));
        }
        assertEquals(0, usersWithEmail("new.user@example.com"));
    }

    // Jackson reads "" as null, so a blank date is reported as missing rather than badly formatted
    @Test
    void aMissingOrBlankDateOfBirthGetsTheRequiredMessage() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("dateOfBirth");
        register(body).andExpect(status().isBadRequest()).andExpect(content().string("Date of Birth is Required"));
        register(with("dateOfBirth", "")).andExpect(status().isBadRequest()).andExpect(content().string("Date of Birth is Required"));
    }

    // ---- Date of birth: range (today is 2024-09-24) ----

    @Test
    void someoneTurning18TodayMayRegister() throws Exception {
        register(with("dateOfBirth", "2006-09-24")).andExpect(status().isCreated());
    }

    @Test
    void someoneTurning18TomorrowGets400() throws Exception {
        register(with("dateOfBirth", "2006-09-25"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("You must be at least 18 years old to register."));
    }

    @Test
    void aFutureDateOfBirthGets400() throws Exception {
        register(with("dateOfBirth", "2030-01-01"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("You must be at least 18 years old to register."));
    }

    @Test
    void exactly120YearsAgoMayRegister() throws Exception {
        register(with("dateOfBirth", "1904-09-24")).andExpect(status().isCreated());
    }

    @Test
    void moreThan120YearsAgoGets400() throws Exception {
        register(with("dateOfBirth", "1904-09-23"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Date of birth cannot be more than 120 years ago."));
    }

    // ---- Bodies Jackson cannot read ----

    @Test
    void aFieldOfTheWrongTypeGets400NamingTheField() throws Exception {
        register(with("name", Map.of("first", "New")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldName").value("name"))
            .andExpect(jsonPath("$.fieldMessage").value("name has the wrong type"));
    }

    @Test
    void malformedJsonGets400() throws Exception {
        register("{\"name\": \"New User\", ")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldMessage").value("Request body is not valid JSON"));
    }
}
