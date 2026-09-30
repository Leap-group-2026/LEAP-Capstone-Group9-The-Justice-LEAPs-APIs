package main.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// Prints the Swagger UI link once the app is ready, right after "Started Application"
@Component
public class StartupUrlLogger {
    private static final Logger log = LoggerFactory.getLogger(StartupUrlLogger.class);

    private final Environment environment;

    public StartupUrlLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSwaggerUrl() {
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        log.info("Swagger UI: http://localhost:{}/swagger-ui.html", port);
    }
}
