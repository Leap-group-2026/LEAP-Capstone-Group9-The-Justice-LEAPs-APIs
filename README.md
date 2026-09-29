# LEAP-Capstone-Group9-The-Justice-LEAPs-APIs

## Running the API

```
mvn spring-boot:run
```

The app starts on port **8081** and connects to the Postgres configured in `src/main/resources/application.properties`.

## API documentation (Swagger)

- Swagger UI: http://localhost:8081/swagger-ui.html
- Raw OpenAPI JSON: http://localhost:8081/v3/api-docs

The docs are generated from the controllers and their annotations every time the app starts, so there's nothing to maintain by hand. To document a new endpoint, give it an `@Operation` and `@ApiResponses`, and add it to `ENDPOINTS` in `src/test/ApiDocsTest.java`. That test fails if an endpoint is missing from the docs. A new endpoint group also needs an entry in the `tags` list in `src/main/config/OpenApiConfig.java`, which sets the order the groups appear in and their descriptions.
