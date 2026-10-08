package main;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import config.JwtAuthenticationFilter;
import config.JwtAuthenticationEntryPoint;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;


@Configuration
public class SecurityConfig {
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
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/user/login").permitAll()
                .requestMatchers("/user").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasRole("CLIENT")
                .requestMatchers("/positions/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/positions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/accounts").hasRole("CLIENT")
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
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
