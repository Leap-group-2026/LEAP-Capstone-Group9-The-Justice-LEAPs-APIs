package test.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestClockConfig {
    
    @Bean
    @Primary
    public Clock testClock() {
        // Return a fixed clock set to 12:00 PM ET on a business day (2024-09-24)
        // 2024-09-24 12:00:00 ET = 2024-09-24 16:00:00 UTC
        Instant instant = Instant.parse("2024-09-24T16:00:00Z");
        return Clock.fixed(instant, ZoneId.of("America/New_York"));
    }
}
