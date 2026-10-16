import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import main.Application;

// MockMvc never performs the ERROR dispatch, so this needs a real server; fixture rows are committed and cleaned up
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "jwt.secret=" + ErrorDispatchRealServerTest.SECRET)
public class ErrorDispatchRealServerTest {

    static final String SECRET = "error-dispatch-test-secret-at-least-32-bytes";
    private static final int CLIENT_ID = 930601;
    private static final int STOCK_ID = 930601;
    private static final int FOREX_ID = 930602;

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID sid;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update(
            "INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash) " +
            "VALUES (?, 'Error Dispatch', 'error.dispatch@example.com', DATE '1990-01-01', '1 Test St', 'ssn-930601', 'pass')", CLIENT_ID);
        sid = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
            "INSERT INTO user_sessions (session_id, user_id, role, created_at, last_activity_at, expires_at) VALUES (?, ?, 'client', ?, ?, ?)",
            sid, CLIENT_ID, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now.plus(Duration.ofHours(8))));
        jdbcTemplate.update("INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'ERRDSP', 'STOCK', 'Error Dispatch Co', 'USD')", STOCK_ID);
        jdbcTemplate.update("INSERT INTO instruments (instrument_id, ticker, asset_type, asset_name, currency) " +
            "VALUES (?, 'ERR/USD', 'FOREX', 'Error Dispatch FX', 'USD')", FOREX_ID);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM instruments WHERE instrument_id IN (?, ?)", STOCK_ID, FOREX_ID);
        jdbcTemplate.update("DELETE FROM user_sessions WHERE user_id = ?", CLIENT_ID);
        jdbcTemplate.update("DELETE FROM user_info WHERE user_id = ?", CLIENT_ID);
    }

    private String clientToken() {
        return Jwts.builder()
            .subject(String.valueOf(CLIENT_ID))
            .claim("role", "client")
            .claim("type", "access")
            .claim("sid", sid.toString())
            .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(30))))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), Jwts.SIG.HS256)
            .compact();
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    void aPlainTickerAnswers200() {
        assertEquals(200, get("/instruments/ERRDSP", clientToken()).getStatusCode().value());
    }

    @Test
    void aTickerWithASlashAnswers200() {
        ResponseEntity<String> response = get("/instruments/ERR/USD", clientToken());

        assertEquals(200, response.getStatusCode().value(), response.getBody());
        assertTrue(response.getBody().contains("\"ticker\":\"ERR/USD\""), response.getBody());
    }

    @Test
    void anUnknownSlashTickerIs404Not401() {
        assertEquals(404, get("/instruments/NOPE/USD", clientToken()).getStatusCode().value());
    }

    @Test
    void anUnmappedPathIs404Not401() {
        ResponseEntity<String> response = get("/instruments/A/B/C", clientToken());

        assertEquals(404, response.getStatusCode().value(), response.getBody());
    }

    @Test
    void withoutATokenTheSamePathsAre401() {
        for (String path : new String[] {"/instruments/ERR/USD", "/instruments/A/B/C", "/instruments/ERRDSP"}) {
            ResponseEntity<String> response = get(path, null);
            assertEquals(401, response.getStatusCode().value(), path);
            assertTrue(response.getBody().contains("\"error\":\"unauthenticated\""), path + ": " + response.getBody());
        }
    }
}
