package config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import exception.SessionRejectedException;
import services.SessionService;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    static final int MIN_SECRET_BYTES = 32;
    public static final String STORE_UNAVAILABLE = "session_store_unavailable";

    private final SecretKey key;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(@Value("${jwt.secret:}") String jwtSecret, SessionService sessionService) {
        byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret is missing or shorter than " + MIN_SECRET_BYTES + " bytes. "
                + "Set the JWT_SECRET environment variable to the same value as authService/.env's JWT_SECRET.");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            if (!authenticate(header, request, response)) {
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean authenticate(String header, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = header.startsWith("Bearer ") ? header.substring(7).trim() : "";
        if (token.isEmpty()) {
            JwtAuthenticationEntryPoint.reject(request, JwtAuthenticationEntryPoint.UNAUTHENTICATED,
                "Authorization header must be \"Bearer <access token>\"");
            return true;
        }

        Claims claims = verifiedAccessClaims(token, request);
        if (claims == null) {
            return true;
        }
        String role = claims.get("role", String.class);
        String userId = claims.getSubject();
        Object sid = claims.get("sid");

        try {
            sessionService.checkAndTouch((String) sid, role, userId);
        } catch (SessionRejectedException e) {
            JwtAuthenticationEntryPoint.reject(request, e.getCode(), e.getMessage());
            return true;
        } catch (RuntimeException e) {
            logger.error("Session check could not run: {}", e.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + STORE_UNAVAILABLE + "\"}");
            return false;
        }

        List<SimpleGrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        return true;
    }

    private Claims verifiedAccessClaims(String token, HttpServletRequest request) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            JwtAuthenticationEntryPoint.reject(request, JwtAuthenticationEntryPoint.TOKEN_EXPIRED, "Access token has expired");
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            JwtAuthenticationEntryPoint.reject(request, JwtAuthenticationEntryPoint.TOKEN_INVALID, "Access token is invalid");
            return null;
        }

        boolean wellFormed = "access".equals(claims.get("type"))
            && claims.getSubject() != null
            && claims.get("role") instanceof String
            && claims.get("sid") instanceof String;
        if (!wellFormed) {
            JwtAuthenticationEntryPoint.reject(request, JwtAuthenticationEntryPoint.TOKEN_INVALID,
                "Access token is missing type, sub, role or sid");
            return null;
        }
        return claims;
    }
}
