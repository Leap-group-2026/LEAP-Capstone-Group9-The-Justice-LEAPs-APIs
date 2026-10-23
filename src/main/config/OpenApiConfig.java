package config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Configuration
public class OpenApiConfig {


    public static final String BEARER_SCHEME = "bearerAuth";
    public static final String INTERNAL_KEY_SCHEME = "internalApiKey";

    // Display order of the groups in the docs. springdoc collects tags into a HashSet, so without
    // sortTags below the order would depend on each tag's hash, not on this list.
    private static final List<Tag> TAGS = List.of(
        new Tag().name("Users").description("Customer registration, login, password reset and profile updates"),
        new Tag().name("Transactions").description("Deposits, withdrawals and trade settlements recorded against an account"),
        new Tag().name("Orders").description("Place BUY and SELL orders against an account"),
        new Tag().name("Accounts").description("Trading accounts: open, look up and close (soft delete) an account belonging to a user"),
        new Tag().name("Instruments").description("Tradable instruments (reference data)"),
        new Tag().name("Positions").description("Holdings of an instrument within an account"),
        new Tag().name("Portfolio Snapshots").description("Daily snapshots of an account's portfolio value"),
        new Tag().name("Sessions (internal)").description("Session checks for the auth service only; they need the internal API key, not a token"),
        new Tag().name("Admin").description("Administrator accounts: create and log in"));

    @Bean
    public OpenAPI leapOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Ribbit Trading Platform API")
            .description("Backend for the LEAP retail trading platform: users, accounts, orders, positions and order history.\n\n"
                + "Almost every endpoint needs an access token. Spring does not issue tokens: get one from the auth "
                + "service with `POST http://localhost:3001/auth/login` (clients) or `/auth/adminLogin` (admins), then "
                + "click **Authorize** and paste the `accessToken`. It lasts 30 minutes; trade the `refreshToken` at "
                + "`POST http://localhost:3001/auth/refresh` for a new one. The auth service's own docs are at "
                + "http://localhost:3001/api/docs.\n\n"
                + "Every request also needs the token's session to be live: a session ends at logout, on a password reset or "
                + "admin force-logout, after 30 minutes without activity, and 8 hours after login. A 401 body is "
                + "`{ \"error\": \"<code>\", \"message\": \"...\" }`; codes starting `session_` mean log in again. "
                + "503 `session_store_unavailable` means the check could not run: retry, the session has not ended.")
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
            .tags(new ArrayList<>(TAGS));
    }

    @Bean
    public OpenApiCustomizer sortTags() {
        List<String> order = TAGS.stream().map(Tag::getName).toList();
        // tags not in TAGS go last
        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.getTags().sort(Comparator.comparingInt(t -> {
                    int i = order.indexOf(t.getName());
                    return i < 0 ? Integer.MAX_VALUE : i;
                }));
            }
        };
    }
}
