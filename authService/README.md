# Auth Service

Logs clients and admins in through Spring and returns a 30-minute JWT plus a refresh token. Spring owns the session (BR-03): it ends 8 hours after login at the latest, after 30 minutes without activity, at logout, on a password reset or an admin force-logout. Spring checks the session on every request, so an ended session's access token is refused at once.

```bash
cd authService && npm install
cp .env.example .env      # set JWT_SECRET, DATABASE_URL and INTERNAL_API_KEY (all required)
npm run start:dev         # http://localhost:3001/health
npm test                  # unit tests
npm run test:e2e          # needs Docker: starts a throwaway Postgres, never the shared one
```

Endpoints:

- `POST /auth/login`, `POST /auth/adminLogin`: `{ email, password }` → `{ accessToken, refreshToken }`
- `POST /auth/refresh`: `{ refreshToken }` → a new `{ accessToken, refreshToken }`. No Authorization header needed, so it works after the access token has expired. Spring is asked first whether the session is still live (401 if not, 502 if Spring is down, and nothing is rotated either way). Each refresh token works once; sending a used one again ends the whole session, here and at Spring.
- `POST /auth/logout`: Bearer access token; ends the session here and at Spring, so the refresh token stops working and Spring refuses the access token on its next request (`401 session_revoked`). 502 if Spring cannot be reached; the refresh token is revoked anyway
- `POST /auth/register`: forwarded to Spring
- `GET /auth/verify`: Bearer access token; 200 only while Spring holds its session live

Refresh tokens are random strings, not JWTs. Only their SHA-256 hash is stored, in the `refresh_tokens` table; its definition is in `test/fixtures/auth-schema.sql`.

Every call to Spring (login, admin login, register, and the session check and revoke under `/internal/sessions/`) carries `X-Internal-Api-Key: <INTERNAL_API_KEY>`. Spring refuses those endpoints without it, so give Spring the same value.

Access tokens carry `sub`, `role`, `type: "access"`, `sid` (Spring's session id), `auth_time`, `iat` and `exp`; `exp` is never later than the session's absolute end. `JWT_MAX_SESSION` no longer exists: Spring's `session.max-lifetime` sets the cap.
