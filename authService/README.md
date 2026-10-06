# Auth Service Docs

1. Install: `cd authService && npm install`
2. Env: `cp .env.example .env`, then fill in the values. `JWT_SECRET` is required and must match Spring's.
   Defaults: `PORT=3001`, `JWT_EXPIRATION=1800` (seconds), `SPRINGBOOT_API_URL=http://localhost:8081`, `CORS_ORIGIN=http://localhost:4200`.
3. Run: `npm run start:dev`, then check `curl localhost:3001/health`
4. Test: `npm test` (unit) and `npm run test:e2e` (login/register, Spring faked; no Spring needed)

## Manual test (Spring running on 8081)

```bash
read -s -p "Password: " PW; echo
TOKEN=$(curl -s -X POST localhost:3001/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"YOUR_EMAIL\",\"password\":\"$PW\"}" | node -pe 'JSON.parse(require("fs").readFileSync(0)).accessToken')
curl -s localhost:3001/auth/verify -H "Authorization: Bearer $TOKEN"; echo   # {"user":{"sub":"<id>","role":"client",...}}
```

Don't test `/auth/register` against real Spring: it creates a row in the shared database.
