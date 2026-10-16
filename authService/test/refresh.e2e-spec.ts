import { BadGatewayException, INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { Pool } from 'pg';
import request from 'supertest';
import { randomUUID } from 'crypto';
import { SpringUsersClient } from '../src/auth/spring-users.client';
import { hashRefreshToken } from '../src/auth/refresh-token.store';
import { ADMIN_ID, CLIENT_ID, resetAuthTables } from './auth-tables';

const spring = {
  login: jest.fn(), adminLogin: jest.fn(), register: jest.fn(), checkSession: jest.fn(), revokeSession: jest.fn(),
};
const reply = (status: number, data: unknown) => ({ status, data, headers: { 'content-type': 'application/json' } });

const springSessions = new Map<string, string>();
const newSpringSession = (secondsLeft = 8 * 3600) => {
  const sid = randomUUID();
  const expiresAt = new Date(Math.floor(Date.now() / 1000) * 1000 + secondsLeft * 1000).toISOString();
  springSessions.set(sid, expiresAt);
  return { sid, expiresAt };
};

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
    springSessions.clear();
    spring.login.mockImplementation(async () => reply(200, { id: CLIENT_ID, ...newSpringSession() }));
    spring.adminLogin.mockImplementation(async () => reply(200, { id: ADMIN_ID, role: 'admin', ...newSpringSession() }));
    spring.checkSession.mockImplementation(async (sid: string) => springSessions.has(sid)
      ? reply(200, { expiresAt: springSessions.get(sid) })
      : reply(401, { error: 'session_invalid', message: 'Session not found' }));
    spring.revokeSession.mockResolvedValue(reply(204, ''));
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

    it("uses Spring's session: its sid in the token and the row, its absolute end on the row", async () => {
      const tokens = await login();

      const row = await rowFor(tokens.refreshToken);
      const claims = jwt.verify(tokens.accessToken);
      const [[sid, expiresAt]] = [...springSessions.entries()];
      expect(claims.sid).toBe(sid);
      expect(row.session_id).toBe(sid);
      expect(row.expires_at.toISOString()).toBe(expiresAt);
      expect(claims.auth_time * 1000).toBe(row.auth_time.getTime());
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
    it('swaps the refresh token for a new pair in the same session, after Spring confirms it is live', async () => {
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
      expect(spring.checkSession).toHaveBeenCalledTimes(1);
      expect(spring.checkSession).toHaveBeenCalledWith(firstRow.session_id);
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
      const sid = (await rowFor(first.refreshToken)).session_id;
      expect(spring.revokeSession).toHaveBeenCalledWith(sid, 'refresh_token_reuse');
      // The legitimate holder's newer token dies with the session
      await refresh(second.refreshToken).expect(401);
    });

    it('still answers 401 to a reused token when Spring cannot be told', async () => {
      const first = await login();
      await refresh(first.refreshToken).expect(200);
      spring.revokeSession.mockRejectedValue(new BadGatewayException('Account service is unavailable'));

      await refresh(first.refreshToken).expect(401);

      expect(await liveRows()).toHaveLength(0);
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
      expect(spring.checkSession).not.toHaveBeenCalled();
    });

    it.each([['session_revoked'], ['session_idle'], ['session_expired']])(
      'refuses and does not rotate when Spring says %s', async (code) => {
        const { refreshToken } = await login();
        spring.checkSession.mockResolvedValue(reply(401, { error: code, message: 'ended' }));

        const res = await refresh(refreshToken).expect(401);

        expect(res.body.accessToken).toBeUndefined();
        expect(res.body.message).toBe('Session has ended, please log in again');
        const rows = await allRows();
        expect(rows).toHaveLength(1);
        expect(rows[0].revoked_at).not.toBeNull();
      });

    it.each([
      ['unreachable', () => spring.checkSession.mockRejectedValue(new BadGatewayException('Account service is unavailable'))],
      ['answering 503', () => spring.checkSession.mockResolvedValue(reply(503, { error: 'session_store_unavailable' }))],
    ])('answers 502 and rotates nothing when Spring is %s, so the same token works later', async (_name, breakSpring) => {
      const { refreshToken } = await login();
      breakSpring();

      await refresh(refreshToken).expect(502);

      const rows = await allRows();
      expect(rows).toHaveLength(1);
      expect(rows[0].revoked_at).toBeNull();

      spring.checkSession.mockImplementation(async (sid: string) => reply(200, { expiresAt: springSessions.get(sid) }));
      await refresh(refreshToken).expect(200);
    });

    it("clamps the new access token to the session's end and keeps Spring's end on the new row", async () => {
      const { refreshToken } = await login();
      const soon = new Date(Math.floor(Date.now() / 1000) * 1000 + 600_000).toISOString();
      spring.checkSession.mockResolvedValue(reply(200, { expiresAt: soon }));

      const res = await refresh(refreshToken).expect(200);

      expect(jwt.decode(res.body.accessToken).exp).toBe(new Date(soon).getTime() / 1000);
      expect((await rowFor(res.body.refreshToken)).expires_at.toISOString()).toBe(soon);
    });

    it.each([[undefined], [''], [42]])('returns 400 for refreshToken %p', async (value) => {
      await refresh(value).expect(400);
    });
  });

  describe('POST /auth/logout', () => {
    it('ends the session here and at Spring, so its refresh token stops working', async () => {
      const tokens = await login();

      await logout(tokens.accessToken).expect(200);

      expect(await liveRows()).toHaveLength(0);
      expect(spring.revokeSession).toHaveBeenCalledWith(jwt.decode(tokens.accessToken).sid, 'logout');
      await refresh(tokens.refreshToken).expect(401);
    });

    it.each([
      ['unreachable', () => spring.revokeSession.mockRejectedValue(new BadGatewayException('Account service is unavailable'))],
      ['answering 503', () => spring.revokeSession.mockResolvedValue(reply(503, { error: 'session_store_unavailable' }))],
    ])('answers 502 when Spring is %s, but has already ended the session here', async (_name, breakSpring) => {
      const tokens = await login();
      breakSpring();

      await logout(tokens.accessToken).expect(502);

      expect(await liveRows()).toHaveLength(0);
      await refresh(tokens.refreshToken).expect(401);
    });

    it("leaves the user's other sessions alone", async () => {
      const laptop = await login();
      const phone = await login();

      await logout(laptop.accessToken).expect(200);

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
      return jwt.sign({ sub: String(CLIENT_ID), role: 'client', type: 'access', sid: SID, auth_time: now, exp: now + 600, ...extra });
    };
    const verify = (token: string) =>
      request(app.getHttpServer()).get('/auth/verify').set('Authorization', `Bearer ${token}`);

    const SID = '3f2c1d6e-8a4b-4c1d-9e2f-0a1b2c3d4e5f';

    it('accepts a well-formed access token whose session Spring holds live', async () => {
      springSessions.set(SID, new Date(Date.now() + 3600_000).toISOString());

      await verify(accessToken()).expect(200);

      expect(spring.checkSession).toHaveBeenCalledWith(SID);
    });

    it('rejects a token whose session has ended at Spring', async () => {
      spring.checkSession.mockResolvedValue(reply(401, { error: 'session_revoked', message: 'ended' }));

      await verify(accessToken()).expect(401);
    });

    it('answers 502, not 401, when Spring is unreachable', async () => {
      spring.checkSession.mockRejectedValue(new BadGatewayException('Account service is unavailable'));

      await verify(accessToken()).expect(502);
    });

    it('rejects a token with a tampered signature', async () => {
      await verify(accessToken().slice(0, -1) + 'X').expect(401);
    });

    it.each([['refresh'], [undefined]])('rejects type %p', async (type) => {
      await verify(accessToken({ type })).expect(401);
    });
  });
});
