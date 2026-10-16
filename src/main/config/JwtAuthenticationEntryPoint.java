package config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    public static final String UNAUTHENTICATED = "unauthenticated";
    public static final String TOKEN_EXPIRED = "token_expired";
    public static final String TOKEN_INVALID = "token_invalid";

    private static final String REASON = JwtAuthenticationEntryPoint.class.getName() + ".REASON";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    record AuthError(String error, String message) {}

    static void reject(HttpServletRequest request, String code, String message) {
        request.setAttribute(REASON, new AuthError(code, message));
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        Object reason = request.getAttribute(REASON);
        AuthError error = reason instanceof AuthError recorded
            ? recorded
            : new AuthError(UNAUTHENTICATED, "Log in to use this endpoint");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(MAPPER.writeValueAsString(error));
    }
}
