import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import main.Application;
import org.springframework.context.annotation.Import;
import test.config.TestSecurityConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Keeps the generated OpenAPI docs complete: every controller group and every endpoint must appear.
 * Adding an endpoint means adding it to ENDPOINTS here, which is the point.
 */
@SpringBootTest(classes = Application.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
public class ApiDocsTest {

    // The order Swagger UI shows the groups in (set in OpenApiConfig)
    private static final List<String> TAGS = List.of("Users", "Transactions", "Orders", "Accounts", "Instruments", "Positions", "Admin");

    // method -> path, one entry per endpoint found by scanning the controllers
    private static final List<Map.Entry<String, String>> ENDPOINTS = List.of(
        Map.entry("get", "/accounts/{id}"),
        Map.entry("get", "/accounts/user/{userId}"),
        Map.entry("get", "/accounts/orders/{accountId}"),
        Map.entry("post", "/accounts"),
        Map.entry("patch", "/accounts/close/{id}"),
        Map.entry("post", "/orders"),
        Map.entry("get", "/orders/{orderId}"),
        Map.entry("post", "/orders/{orderId}/cancel"),
        Map.entry("get", "/orders/account/{accountId}"),
        Map.entry("get", "/orders/{orderId}/history"),
        Map.entry("get", "/transactions/account/{accountId}"),
        Map.entry("get", "/instruments/{ticker}"),
        Map.entry("get", "/user/accounts"),
        Map.entry("get", "/instruments"),
        Map.entry("get", "/positions/{id}"),
        Map.entry("post", "/positions"),
        Map.entry("get", "/positions/account/{accountId}"),
        Map.entry("post", "/admin"),
        Map.entry("post", "/admin/login"),
        Map.entry("get", "/admin/orders"),
        Map.entry("get", "/admin/orders/{userId}"),
        Map.entry("get", "/admin/orders/{userId}/cancelled"),
        Map.entry("get", "/admin/accounts"),
        Map.entry("get", "/admin/accounts/{userId}"),
        Map.entry("get", "/admin/users"),
        Map.entry("patch", "/user"),
        Map.entry("post", "/user"),
        Map.entry("post", "/user/login"),
        Map.entry("post", "/user/resetpassword"),
        Map.entry("post", "/user/resetpassword/reset"),
        Map.entry("post", "/user/transactions/exchange"));

    // The endpoints only the auth service calls: they take the internal API key instead of a token (InternalApiKeyFilter)
    private static final List<Map.Entry<String, String>> INTERNAL_ENDPOINTS = List.of(
        Map.entry("post", "/user"),
        Map.entry("post", "/user/login"),
        Map.entry("post", "/admin/login"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    void everyControllerGroupIsDocumentedInOrder() throws Exception {
        List<String> tags = new ArrayList<>();
        apiDocs().path("tags").forEach(t -> {
            tags.add(t.path("name").asText());
            assertFalse(t.path("description").asText().isBlank(), "group has no description: " + t.path("name"));
        });
        assertEquals(TAGS, tags);
    }

    @Test
    void everyEndpointIsDocumentedExactlyOnceWithASummary() throws Exception {
        JsonNode paths = apiDocs().path("paths");
        int documented = 0;
        for (Iterator<String> it = paths.fieldNames(); it.hasNext(); ) {
            documented += paths.path(it.next()).size();
        }
        assertEquals(ENDPOINTS.size(), documented, "endpoint count in /v3/api-docs differs from the controllers");

        for (Map.Entry<String, String> e : ENDPOINTS) {
            JsonNode op = paths.path(e.getValue()).path(e.getKey());
            String name = e.getKey().toUpperCase() + " " + e.getValue();
            assertFalse(op.isMissingNode(), "not documented: " + name);
            assertEquals(1, op.path("tags").size(), name + " should be in exactly one group, has " + op.path("tags"));
            assertFalse(op.path("summary").asText().isBlank(), name + " has no summary");
        }
    }

    @Test
    void definesTheBearerSchemeAndRequiresItByDefault() throws Exception {
        JsonNode docs = apiDocs();

        JsonNode scheme = docs.path("components").path("securitySchemes").path("bearerAuth");
        assertEquals("http", scheme.path("type").asText());
        assertEquals("bearer", scheme.path("scheme").asText());
        assertTrue(docs.path("security").toString().contains("bearerAuth"), "bearerAuth is not the default: " + docs.path("security"));
    }

    @Test
    void theAuthServiceEndpointsAskForTheInternalKeyAndEveryOtherForAToken() throws Exception {
        JsonNode docs = apiDocs();
        JsonNode keyScheme = docs.path("components").path("securitySchemes").path("internalApiKey");
        assertEquals("apiKey", keyScheme.path("type").asText());
        assertEquals("header", keyScheme.path("in").asText());
        assertEquals("X-Internal-Api-Key", keyScheme.path("name").asText());

        JsonNode paths = docs.path("paths");
        for (Map.Entry<String, String> e : ENDPOINTS) {
            JsonNode op = paths.path(e.getValue()).path(e.getKey());
            String name = e.getKey().toUpperCase() + " " + e.getValue();
            if (INTERNAL_ENDPOINTS.contains(e)) {
                // Exactly the internal key, and no bearer token
                assertEquals("[{\"internalApiKey\":[]}]", op.path("security").toString(), name + " should ask for the internal key only");
            } else {
                // No security of its own, so it inherits the global bearer requirement
                assertFalse(op.has("security"), name + " should need an access token, has " + op.path("security"));
            }
        }
    }
}
