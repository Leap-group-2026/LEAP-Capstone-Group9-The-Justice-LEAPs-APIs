import { BadGatewayException, INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import request from 'supertest';
import { SpringUsersClient } from '../src/auth/spring-users.client';


// Spring is replaced by this fake, so each case controls exactly what Spring "answers"
const spring = { login: jest.fn(), register: jest.fn() };

const reply = (status: number, data: unknown, contentType = 'application/json') =>
  ({ status, data, headers: { 'content-type': contentType } });

describe('POST /auth/login and /auth/register (N2)', () => {
  let app: INestApplication;
  let jwt: JwtService;

  beforeAll(async () => {
    // Set before AppModule loads: ConfigModule.forRoot reads the environment at import time
    process.env.JWT_SECRET = 'test-secret';
    process.env.JWT_EXPIRATION = '1800';
    const { AppModule } = await import('../src/app.module');

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(SpringUsersClient)
      .useValue(spring)
      .compile();
    app = moduleRef.createNestApplication();
    await app.init();
    jwt = app.get(JwtService);
  });

  afterAll(() => app.close());
  beforeEach(() => jest.resetAllMocks());

  const login = (body: object) => request(app.getHttpServer()).post('/auth/login').send(body);

  describe('login', () => {
    it('returns 200 with a token for sub = user id, role = client, lasting 1800 seconds', async () => {
      spring.login.mockResolvedValue(reply(200, { id: 42 }));

      const res = await login({ email: 'jo@example.com', password: 'Secret' }).expect(200);

      expect(Object.keys(res.body)).toEqual(['accessToken']);
      const claims = jwt.verify(res.body.accessToken);
      expect(claims.sub).toBe('42');
      expect(claims.role).toBe('client');
      expect(claims.exp - claims.iat).toBe(1800);
      expect(spring.login).toHaveBeenCalledWith('jo@example.com', 'Secret');
    });

    it('puts nothing but sub, role, iat and exp in the token', async () => {
      // Even if Spring were to send extra fields, none of them may reach the token
      spring.login.mockResolvedValue(reply(200, { id: 42, email: 'jo@example.com', passHash: 'x', ssn: '123' }));

      const res = await login({ email: 'jo@example.com', password: 'Secret' }).expect(200);

      expect(Object.keys(jwt.decode(res.body.accessToken)).sort())
        .toEqual(['auth_time', 'exp', 'iat', 'role', 'sub', 'type']);
    });

    it('returns the same 401 body for a wrong password and an unknown email', async () => {
      // Spring's wording differs on purpose: NestJS itself must not let the difference through
      spring.login
        .mockResolvedValueOnce(reply(401, 'Wrong password', 'text/plain'))
        .mockResolvedValueOnce(reply(401, "Email doesn't exist", 'text/plain'));

      const wrongPassword = await login({ email: 'jo@example.com', password: 'nope' }).expect(401);
      const unknownEmail = await login({ email: 'nobody@example.com', password: 'Secret' }).expect(401);

      expect(wrongPassword.body).toEqual(unknownEmail.body);
      expect(wrongPassword.body.accessToken).toBeUndefined();
    });

    it("passes a locked account's status and message through, with no token", async () => {
      spring.login.mockResolvedValue(reply(423, 'Account locked', 'text/plain'));

      const res = await login({ email: 'jo@example.com', password: 'Secret' }).expect(423);

      expect(res.body.message).toBe('Account locked');
      expect(res.body.accessToken).toBeUndefined();
    });

    it.each([
      ['no email', { password: 'Secret' }],
      ['no password', { email: 'jo@example.com' }],
      ['empty body', {}],
      ['blank email', { email: '', password: 'Secret' }],
    ])('returns 400 without calling Spring for %s', async (_name, body) => {
      await login(body).expect(400);

      expect(spring.login).not.toHaveBeenCalled();
    });

    it('returns 502 and no token when Spring is unreachable', async () => {
      spring.login.mockRejectedValue(new BadGatewayException('Account service is unavailable'));

      const res = await login({ email: 'jo@example.com', password: 'Secret' }).expect(502);

      expect(res.body.accessToken).toBeUndefined();
    });

    it('returns 502 and no token when Spring fails with a 500', async () => {
      spring.login.mockResolvedValue(reply(500, 'boom', 'text/plain'));

      const res = await login({ email: 'jo@example.com', password: 'Secret' }).expect(502);

      expect(res.body.accessToken).toBeUndefined();
    });
  });

  describe('register', () => {
    const register = (body: object) => request(app.getHttpServer()).post('/auth/register').send(body);

    it("forwards the body unchanged and returns Spring's 201 and body", async () => {
      const created = { userId: 7, name: 'Jo', email: 'jo@example.com' };
      spring.register.mockResolvedValue(reply(201, created));
      const body = { name: 'Jo', email: 'jo@example.com', password: 'LongEnough!!AA', ssn: '123', extra: 'kept' };

      const res = await register(body).expect(201);

      expect(res.body).toEqual(created);
      expect(spring.register).toHaveBeenCalledWith(body);
    });

    it("returns Spring's 400 and message for a weak password", async () => {
      spring.register.mockResolvedValue(reply(400, 'Password must be a minimum of 12 characters.', 'text/plain'));

      const res = await register({ email: 'jo@example.com', password: 'short' }).expect(400);

      expect(res.text).toBe('Password must be a minimum of 12 characters.');
    });
  });
});
