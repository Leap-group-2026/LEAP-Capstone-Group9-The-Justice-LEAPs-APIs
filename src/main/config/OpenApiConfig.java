package config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI leapOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Ribbit Trading Platform API")
            .description("Backend for the LEAP retail trading platform: users, accounts, orders, positions and order history.")
            .version("1.0.0"))
            // Swagger UI shows the groups in this order. Defining a group here replaces the
            // description on the controller's @Tag, so the descriptions live here too.
            .tags(List.of(
                new Tag().name("Admin").description("Administrator accounts: create and log in"),
                new Tag().name("Users").description("Customer registration, login, password reset and profile updates"),
                new Tag().name("Accounts").description("Trading accounts: open, look up and close (soft delete) an account belonging to a user"),
                new Tag().name("Orders").description("Place BUY and SELL orders against an account"),
                new Tag().name("Instruments").description("Tradable instruments (reference data)"),
                new Tag().name("Positions").description("Holdings of an instrument within an account"),
                new Tag().name("Transactions").description("Deposits, withdrawals and trade settlements recorded against an account")
            ));
    }
}
