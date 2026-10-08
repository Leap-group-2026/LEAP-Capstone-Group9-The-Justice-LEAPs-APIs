import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { Pool } from 'pg';
import request from 'supertest';
import { SpringUsersClient } from '../src/auth/spring-users.client';
import { hashRefreshToken } from '../src/auth/refresh-token.store';
import { ADMIN_ID, CLIENT_ID, resetAuthTables } from './auth-tables';

const spring = { login: jest.fn(), adminLogin: jest.fn(), register: jest.fn() };
const reply = (status: number, data: unknown) => ({ status, data, headers: { 'content-type': 'application/json' } });

/**
 * Refresh tokens against a real (throwaway) Postgres. Assertions read refresh_tokens directly rather than
 * trusting the store, so they prove what was actually written, not just that the writer and reader agree.
 */
describe('refresh tokens', () => {
  let app: INestApplication;
  let jwt: JwtService;
  let db: Pool;

  beforeAll(async () => {
    process.env.JWT_SECRET = 'test-secret';
    process.env.INTERNAL_API_KEY = 'test-only-internal-api-key-0123456789-abcdef';
    process.env.DATABASE_URL = process.env.TEST_DATABASE_URL;
    process.env.JWT_EXPIRATION = '1800';
    process.env.JWT_MAX_SESSION = '28800';
    const { AppModule } = await import('../src/app.module');
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(SpringUsersClient).useValue(spring)
      .compile();
    app = moduleRef.createNestApplication();
    await app.init();
    jwt = app.get(JwtService);
    db = new Pool({ connectionString: process.env.TEST_DATABASE_URL });
  });

  afterAll(async () => {
    await app.close();
    await db.end();
  });

  beforeEach(async () => {
    jest.resetAllMocks();
    spring.login.mockResolvedValue(reply(200, { id: CLIENT_ID }));
    spring.adminLogin.mockResolvedValue(reply(200, { id: ADMIN_ID }));
    await resetAuthTables(db);
  });

  const credentials = { email: 'jo@example.com', password: 'Secret' };
  const login = async () => (await request(app.getHttpServer()).post('/auth/login').send(credentials).expect(200)).body;
  // Deliberately no Authorization header: the refresh token alone must be enough
  const refresh = (refreshToken: unknown) => request(app.getHttpServer()).post('/auth/refresh').send({ refreshToken });
  const logout = (accessToken: string) =>
    request(app.getHttpServer()).post('/auth/logout').set('Authorization', `Bearer ${accessToken}`);

  const allRows = async () => (await db.query('SELECT * FROM refresh_tokens')).rows;
  const liveRows = async () => (await db.query('SELECT * FROM refresh_tokens WHERE revoked_at IS NULL')).rows;
  const rowFor = async (rawToken: string) =>
    (await db.query('SELECT * FROM refresh_tokens WHERE token_hash = $1', [hashRefreshToken(rawToken)])).rows[0];

  describe('login', () => {
    it('returns a refresh token and stores only its SHA-256 hash, owned by the client', async () => {
      const tokens = await login();

      expect(Object.keys(tokens).sort()).toEqual(['accessToken', 'refreshToken']);
      const rows = await allRows();
      expect(rows).toHaveLength(1);
      expect(rows[0].token_hash).toBe(hashRefreshToken(tokens.refreshToken));
      expect(JSON.stringify(rows)).not.toContain(tokens.refreshToken);
      expect([rows[0].user_id, rows[0].admin_id, rows[0].revoked_at]).toEqual([CLIENT_ID, null, null]);
    });

    it('ties the access token to the stored session and caps the session at JWT_MAX_SESSION', async () => {
      const tokens = await login();

      const row = await rowFor(tokens.refreshToken);
      const claims = jwt.verify(tokens.accessToken);
      expect(claims.sid).toBe(row.session_id);
      expect(claims.auth_time * 1000).toBe(row.auth_time.getTime());
      expect(row.expires_at.getTime() - row.auth_time.getTime()).toBe(28800 * 1000);
    });

    it('gives every login its own session and token', async () => {
      const first = await login();
      const second = await login();

      expect(second.refreshToken).not.toBe(first.refreshToken);
      expect((await rowFor(second.refreshToken)).session_id).not.toBe((await rowFor(first.refreshToken)).session_id);
    });

    it('stores an admin login against admin_id and signs role admin', async () => {
      const tokens = (await request(app.getHttpServer())
        .post('/auth/adminLogin').send({ email: 'admin@example.com', password: 'Secret' }).expect(200)).body;

      const row = await rowFor(tokens.refreshToken);
      expect([row.user_id, row.admin_id]).toEqual([null, ADMIN_ID]);
      const claims = jwt.verify(tokens.accessToken);
      expect([claims.sub, claims.role]).toEqual([String(ADMIN_ID), 'admin']);
    });

    it('clears out expired sessions, but keeps revoked tokens of live ones', async () => {
      const expired = await login();
      await db.query(`UPDATE refresh_tokens SET expires_at = now() - interval '1 second'`);
      const live = await login();
      await refresh(live.refreshToken).expect(200);

      await login();

      expect(await rowFor(expired.refreshToken)).toBeUndefined();
      // Still needed: it is how a replay of this token would be recognised
      expect((await rowFor(live.refreshToken)).revoked_at).not.toBeNull();
    });

    it('stores nothing when Spring rejects the password', async () => {
      spring.login.mockResolvedValue(reply(401, 'Wrong password'));

      await request(app.getHttpServer()).post('/auth/login').send(credentials).expect(401);

      expect(await allRows()).toHaveLength(0);
    });
  });

  describe('POST /auth/refresh', () => {
    it('swaps the refresh token for a new pair in the same session, without asking Spring', async () => {
      const first = await login();
      const firstRow = await rowFor(first.refreshToken);

      const res = await refresh(first.refreshToken).expect(200);

      expect(res.body.refreshToken).not.toBe(first.refreshToken);
      expect((await rowFor(first.refreshToken)).revoked_at).not.toBeNull();
      const live = await liveRows();
      expect(live).toHaveLength(1);
      expect(live[0].token_hash).toBe(hashRefreshToken(res.body.refreshToken));
      // Rotation must not extend the session: same session, same login time, same hard expiry
      expect([live[0].session_id, live[0].auth_time, live[0].expires_at])
        .toEqual([firstRow.session_id, firstRow.auth_time, firstRow.expires_at]);

      const claims = jwt.verify(res.body.accessToken);
      expect([claims.sub, claims.role, claims.type, claims.sid, claims.auth_time])
        .toEqual([String(CLIENT_ID), 'client', 'access', firstRow.session_id, jwt.decode(first.accessToken).auth_time]);
      expect(claims.exp - claims.iat).toBe(1800);
      expect(spring.login).toHaveBeenCalledTimes(1);
    });

    it('keeps rotating: each new refresh token works once in turn', async () => {
      let token = (await login()).refreshToken;

      for (let i = 0; i < 3; i++) {
        token = (await refresh(token).expect(200)).body.refreshToken;
      }

      expect(await liveRows()).toHaveLength(1);
      expect(await allRows()).toHaveLength(4);
    });

    it('treats a reused refresh token as stolen and ends the whole session', async () => {
      const first = await login();
      const second = (await refresh(first.refreshToken).expect(200)).body;

      const replay = await refresh(first.refreshToken).expect(401);

      expect(replay.body.message).toBe('Invalid refresh token');
      expect(await liveRows()).toHaveLength(0);
      // The legitimate holder's newer token dies with the session
      await refresh(second.refreshToken).expect(401);
    });

    it('ends only the session the reused token belonged to', async () => {
      const stolen = await login();
      const otherDevice = await login();
      await refresh(stolen.refreshToken).expect(200);

      await refresh(stolen.refreshToken).expect(401);

      await refresh(otherDevice.refreshToken).expect(200);
    });

    it('lets exactly one of two simultaneous refreshes with the same token through', async () => {
      const { refreshToken } = await login();
      // Hold the row's lock so both requests are guaranteed to be in flight at once; without this they
      // tend to run one after the other, and the test would pass with or without FOR UPDATE
      const blocker = await db.connect();
      await blocker.query('BEGIN');
      await blocker.query('SELECT 1 FROM refresh_tokens WHERE token_hash = $1 FOR UPDATE', [hashRefreshToken(refreshToken)]);

      const pending = Promise.all([refresh(refreshToken), refresh(refreshToken)]);
      const waitingOnLock = async () => Number((await db.query(
        `SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND wait_event_type = 'Lock'`,
      )).rows[0].count);
      const deadline = Date.now() + 5000;
      while ((await waitingOnLock()) < 2 && Date.now() < deadline) {
        await new Promise((resolve) => setTimeout(resolve, 20));
      }
      await blocker.query('COMMIT');
      blocker.release();
      const results = await pending;

      expect(results.map((r) => r.status).sort()).toEqual([200, 401]);
    });

    it('gives an unknown token and a reused token the same 401 body', async () => {
      const { refreshToken } = await login();
      await refresh(refreshToken).expect(200);

      const reused = await refresh(refreshToken).expect(401);
      const unknown = await refresh('not-a-token-we-issued').expect(401);

      expect(reused.body).toEqual(unknown.body);
      expect(unknown.body.accessToken).toBeUndefined();
    });

    it('ends a session past its expiry even though its token was never used', async () => {
      const { refreshToken } = await login();
      await db.query(`UPDATE refresh_tokens SET expires_at = now() - interval '1 second'`);

      const res = await refresh(refreshToken).expect(401);

      expect(res.body.message).toBe('Session has expired, please log in again');
      expect(res.body.accessToken).toBeUndefined();
    });

    it.each([[undefined], [''], [42]])('returns 400 for refreshToken %p', async (value) => {
      await refresh(value).expect(400);
    });
  });

  describe('POST /auth/logout', () => {
    it('ends the session, so its refresh token stops working', async () => {
      const tokens = await login();

      await logout(tokens.accessToken).expect(201);

      expect(await liveRows()).toHaveLength(0);
      await refresh(tokens.refreshToken).expect(401);
    });

    it("leaves the user's other sessions alone", async () => {
      const laptop = await login();
      const phone = await login();

      await logout(laptop.accessToken).expect(201);

      await refresh(phone.refreshToken).expect(200);
    });

    it('rejects an access token that carries no session id', async () => {
      const now = Math.floor(Date.now() / 1000);
      const noSid = jwt.sign({ sub: String(CLIENT_ID), role: 'client', type: 'access', auth_time: now });

      await logout(noSid).expect(401);
    });
  });

  describe('access token checks (GET /auth/verify)', () => {
    const accessToken = (extra: object = {}) => {
      const now = Math.floor(Date.now() / 1000);
      return jwt.sign({ sub: String(CLIENT_ID), role: 'client', type: 'access', sid: 'some-session', auth_time: now, ...extra });
    };
    const verify = (token: string) =>
      request(app.getHttpServer()).get('/auth/verify').set('Authorization', `Bearer ${token}`);

    it('accepts a well-formed access token', async () => {
      await verify(accessToken()).expect(200);
    });

    it('rejects a token with a tampered signature', async () => {
      await verify(accessToken().slice(0, -1) + 'X').expect(401);
    });

    it.each([['refresh'], [undefined]])('rejects type %p', async (type) => {
      await verify(accessToken({ type })).expect(401);
    });
  });
});
