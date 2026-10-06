# Auth Service

Logs clients in through Spring and returns a 30-minute JWT.

```bash
cd authService && npm install
cp .env.example .env      # set JWT_SECRET (required)
npm run start:dev         # http://localhost:3001/health
npm test && npm run test:e2e
```

Endpoints: `POST /auth/login`, `POST /auth/register` (forwarded to Spring), `GET /auth/verify` (needs token).
