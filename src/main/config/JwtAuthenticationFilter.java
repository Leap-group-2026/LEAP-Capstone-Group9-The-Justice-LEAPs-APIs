package config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${jwt.secret:}") String jwtSecret) {
        byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret is missing or shorter than " + MIN_SECRET_BYTES + " bytes. "
                + "Set the JWT_SECRET environment variable to the same value as authService/.env's JWT_SECRET.");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Claims claims = verifiedAccessClaims(extractToken(request));
        if (claims != null) {
            String role = claims.get("role", String.class);
            String userId = claims.getSubject();
            if (role != null && userId != null) {
                List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
    }

    private Claims verifiedAccessClaims(String token) {
        if (token == null) {
            return null;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return "access".equals(claims.get("type")) ? claims : null;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return null;
        }
    }
}
