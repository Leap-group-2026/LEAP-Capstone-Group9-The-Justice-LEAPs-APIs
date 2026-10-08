# LEAP-Capstone-Group9-The-Justice-LEAPs-APIs

## Running the API

```
JWT_SECRET="$(grep '^JWT_SECRET=' authService/.env | cut -d= -f2-)" \
INTERNAL_API_KEY="$(grep '^INTERNAL_API_KEY=' authService/.env | cut -d= -f2-)" \
mvn spring-boot:run

OR 

add:
spring.config.import=optional:file:./authService/.env[.properties]
jwt.secret=${JWT_SECRET:}

then mvn spring-boot:run
```

The app starts on port **8081** and connects to the Postgres configured in `src/main/resources/application.properties`.

`JWT_SECRET` must be the same value the auth service signs tokens with, and `INTERNAL_API_KEY` the same key the auth service sends; both live in `authService/.env` and must be at least 32 bytes. Without either the app refuses to start.

`POST /user/login`, `POST /admin/login` and `POST /user` are called only by the auth service: they need no token, but refuse any request without the `X-Internal-Api-Key` header (401). Get a token from the auth service (`POST http://localhost:3001/auth/login`) and send it as `Authorization: Bearer <accessToken>`. A missing, expired or invalid token gets 401; a valid token on a route its role can't use gets 403.

## API documentation (Swagger)

- Swagger UI: http://localhost:8081/swagger-ui.html
- Raw OpenAPI JSON: http://localhost:8081/v3/api-docs

The docs are generated from the controllers and their annotations every time the app starts, so there's nothing to maintain by hand. To document a new endpoint, give it an `@Operation` and `@ApiResponses`, and add it to `ENDPOINTS` in `src/test/ApiDocsTest.java`. That test fails if an endpoint is missing from the docs. A new endpoint group also needs an entry in the `tags` list in `src/main/config/OpenApiConfig.java`, which sets the order the groups appear in and their descriptions.
