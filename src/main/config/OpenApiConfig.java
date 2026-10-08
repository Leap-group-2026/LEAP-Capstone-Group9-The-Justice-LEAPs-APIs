package config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {


    public static final String BEARER_SCHEME = "bearerAuth";
    public static final String INTERNAL_KEY_SCHEME = "internalApiKey";

    @Bean
    public OpenAPI leapOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Ribbit Trading Platform API")
            .description("Backend for the LEAP retail trading platform: users, accounts, orders, positions and order history.\n\n"
                + "Almost every endpoint needs an access token. Spring does not issue tokens: get one from the auth "
                + "service with `POST http://localhost:3001/auth/login` (clients) or `/auth/adminLogin` (admins), then "
                + "click **Authorize** and paste the `accessToken`. It lasts 30 minutes; trade the `refreshToken` at "
                + "`POST http://localhost:3001/auth/refresh` for a new one. The auth service's own docs are at "
                + "http://localhost:3001/api/docs.")
            .version("1.0.0"))
            .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("The accessToken from the auth service. Role client or admin decides which endpoints it opens."))
                .addSecuritySchemes(INTERNAL_KEY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-Internal-Api-Key")
                    .description("INTERNAL_API_KEY, shared with the auth service. Only the auth service calls these endpoints.")))

            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
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
