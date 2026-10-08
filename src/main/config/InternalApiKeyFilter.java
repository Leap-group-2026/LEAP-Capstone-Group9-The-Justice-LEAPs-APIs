package config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalApiKeyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Internal-Api-Key";
    static final int MIN_KEY_BYTES = 32;


    private static final Set<String> INTERNAL_ENDPOINTS = Set.of("POST /user/login", "POST /admin/login", "POST /user");

    private final byte[] expectedKey;


    public InternalApiKeyFilter(@Value("${internal.api.key:}") String apiKey) {
        byte[] key = apiKey.getBytes(StandardCharsets.UTF_8);
        if (key.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("internal.api.key is missing or shorter than " + MIN_KEY_BYTES + " bytes. "
                + "Set the INTERNAL_API_KEY environment variable to the same value as authService/.env's INTERNAL_API_KEY.");
        }
        this.expectedKey = key;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !INTERNAL_ENDPOINTS.contains(request.getMethod() + " " + path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String sent = request.getHeader(HEADER);
        if (sent == null || !MessageDigest.isEqual(sent.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Missing or invalid " + HEADER);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
