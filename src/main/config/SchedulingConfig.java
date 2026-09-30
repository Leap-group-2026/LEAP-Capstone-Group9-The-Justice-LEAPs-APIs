package main.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// On unless app.scheduling.enabled=false. Tests turn it off: every cached test context would otherwise run its
// own copy of each @Scheduled job against the shared H2 database and interfere with other tests' data.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
