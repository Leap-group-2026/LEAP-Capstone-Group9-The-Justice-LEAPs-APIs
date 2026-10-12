package main;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import config.JwtAuthenticationFilter;
import config.JwtAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {
    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public Clock clock(){
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/user/login").permitAll()
                .requestMatchers("/user").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasRole("CLIENT")
                .requestMatchers("/positions/{id}").hasRole("ADMIN")
                .requestMatchers("/orders/{orderId}").hasRole("ADMIN")
                .requestMatchers("/transactions/account/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/transactions/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/positions/account/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/positions/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/orders/account/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/orders/{orderId}/history").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/orders/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/accounts/{id}").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/accounts/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/instruments/**").hasAnyRole("ADMIN", "CLIENT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigin));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
