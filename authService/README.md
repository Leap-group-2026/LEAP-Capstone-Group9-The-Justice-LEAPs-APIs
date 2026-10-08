# Auth Service

Logs clients and admins in through Spring and returns a 30-minute JWT plus a refresh token. A session lasts at most 8 hours (`JWT_MAX_SESSION`).

```bash
cd authService && npm install
cp .env.example .env      # set JWT_SECRET and DATABASE_URL (both required)
npm run start:dev         # http://localhost:3001/health
npm test                  # unit tests
npm run test:e2e          # needs Docker: starts a throwaway Postgres, never the shared one
```

Endpoints:

- `POST /auth/login`, `POST /auth/adminLogin`: `{ email, password }` → `{ accessToken, refreshToken }`
- `POST /auth/refresh`: `{ refreshToken }` → a new `{ accessToken, refreshToken }`. No Authorization header needed, so it works after the access token has expired. Each refresh token works once; sending a used one again ends the whole session.
- `POST /auth/logout`: Bearer access token; ends the session, so its refresh token stops working
- `POST /auth/register`: forwarded to Spring
- `GET /auth/verify`: Bearer access token

Refresh tokens are random strings, not JWTs. Only their SHA-256 hash is stored, in the `refresh_tokens` table; its definition is in `test/fixtures/auth-schema.sql`.
