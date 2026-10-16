import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import main.Application;
import repos.SessionRepo;

@SpringBootTest(classes = Application.class, properties = "jwt.secret=" + SessionStoreUnavailableTest.SECRET)
@AutoConfigureMockMvc
@Transactional
public class SessionStoreUnavailableTest {

    static final String SECRET = "session-store-test-secret-at-least-32-bytes-long";
    private static final int CLIENT_ID = 930301;
    private static final int ACCOUNT_ID = 930301;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockBean
    private SessionRepo sessionRepo;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Store Down', 'store.down@example.com', DATE '1990-01-01', '1 Test St', 'ssn-930301', 'pass')", CLIENT_ID);
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, balance, portfolio_size, trade_type, created_at, account_active) " +
            "VALUES (?, ?, 10000.0000, 'BALANCED', 'Passive', now(), TRUE)", ACCOUNT_ID, CLIENT_ID);
        when(sessionRepo.findById(any())).thenThrow(new CannotGetJdbcConnectionException("connection refused"));
    }

    private String clientToken() {
        return Jwts.builder()
            .subject(String.valueOf(CLIENT_ID))
            .claim("role", "client")
            .claim("type", "access")
            .claim("sid", UUID.randomUUID().toString())
            .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(30))))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), Jwts.SIG.HS256)
            .compact();
    }

    @Test
    void aReadIsRefusedWith503() throws Exception {
        mockMvc.perform(get("/accounts/" + ACCOUNT_ID).header("Authorization", "Bearer " + clientToken()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().json("{\"error\":\"session_store_unavailable\"}", true));
    }

    @Test
    void aWriteIsRefusedWith503AndNothingMoves() throws Exception {
        mockMvc.perform(post("/transactions/account/" + ACCOUNT_ID + "/deposit")
                .header("Authorization", "Bearer " + clientToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100,\"fundingMethod\":\"bank\"}"))
            .andExpect(status().isServiceUnavailable());

        assertEquals(0, new BigDecimal("10000").compareTo(jdbcTemplate.queryForObject(
            "SELECT balance FROM accounts WHERE account_id = ?", BigDecimal.class, ACCOUNT_ID)));
        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM transactions WHERE account_id = ?", Integer.class, ACCOUNT_ID));
    }

    @Test
    void aRequestWithoutATokenIsStillA401() throws Exception {
        mockMvc.perform(get("/accounts/" + ACCOUNT_ID)).andExpect(status().isUnauthorized());
    }
}
