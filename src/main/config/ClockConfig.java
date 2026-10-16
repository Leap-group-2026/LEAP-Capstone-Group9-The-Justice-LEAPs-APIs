package config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Not in SecurityConfig: SecurityConfig -> JwtAuthenticationFilter -> SessionService -> Clock would be a cycle
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(){
        return Clock.systemUTC();
    }
}
