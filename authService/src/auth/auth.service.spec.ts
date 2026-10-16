import { BadGatewayException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { randomUUID } from 'crypto';
import { validateEnv } from '../config/env.validation';
import { AuthService } from './auth.service';

describe('AuthService token signing', () => {
  const jwt = new JwtService({ secret: 'test-secret' });
  const config = { get: (key: string) => ({ JWT_EXPIRATION: 1800 } as Record<string, number>)[key] };
  let spring: { login: jest.Mock; adminLogin: jest.Mock };
  let store: { deleteExpired: jest.Mock; create: jest.Mock };
  let service: AuthService;

  const springSession = (secondsLeft: number) => ({
    sid: randomUUID(),
    expiresAt: new Date(Date.now() + secondsLeft * 1000).toISOString(),
  });

  beforeEach(() => {
    spring = { login: jest.fn(), adminLogin: jest.fn() };
    store = { deleteExpired: jest.fn(), create: jest.fn() };
    service = new AuthService(jwt, spring as any, config as any, store as any);
  });

  it('issues tokens that expire JWT_EXPIRATION seconds after they are issued', async () => {
    spring.login.mockResolvedValue({ status: 200, data: { id: 42, ...springSession(8 * 3600) } });

    const { accessToken } = await service.login({ email: 'jo@example.com', password: 'Secret' });
    const { iat, exp } = jwt.decode(accessToken);

    expect(exp - iat).toBe(1800);
  });

  it("uses Spring's sid and stores Spring's expiresAt as the refresh token's end", async () => {
    const session = springSession(8 * 3600);
    spring.login.mockResolvedValue({ status: 200, data: { id: 42, ...session } });

    const { accessToken } = await service.login({ email: 'jo@example.com', password: 'Secret' });

    expect(jwt.decode(accessToken).sid).toBe(session.sid);
    const stored = store.create.mock.calls[0][1];
    expect(stored.sessionId).toBe(session.sid);
    expect(stored.expiresAt.toISOString()).toBe(session.expiresAt);
  });

  it("clamps exp to the session's end when that comes sooner", async () => {
    const session = springSession(600);
    spring.adminLogin.mockResolvedValue({ status: 200, data: { id: 7, role: 'admin', ...session } });

    const { accessToken } = await service.adminLogin({ email: 'admin@example.com', password: 'Secret' });

    expect(jwt.decode(accessToken).exp).toBe(Math.floor(new Date(session.expiresAt).getTime() / 1000));
  });

  it.each([
    ['no sid', { id: 42, expiresAt: new Date(Date.now() + 3600_000).toISOString() }],
    ['a sid that is not a UUID', { id: 42, sid: 'abc', expiresAt: new Date(Date.now() + 3600_000).toISOString() }],
    ['no expiresAt', { id: 42, sid: randomUUID() }],
    ['an unreadable expiresAt', { id: 42, sid: randomUUID(), expiresAt: 'soon' }],
  ])("answers 502 and stores nothing when Spring's login has %s", async (_name, data) => {
    spring.login.mockResolvedValue({ status: 200, data });

    await expect(service.login({ email: 'jo@example.com', password: 'Secret' })).rejects.toBeInstanceOf(BadGatewayException);
    expect(store.create).not.toHaveBeenCalled();
  });
});

describe('validateEnv', () => {
  it('names JWT_SECRET when it is missing', () => {
    expect(() => validateEnv({ JWT_EXPIRATION: '1800' })).toThrow(/JWT_SECRET/);
  });

  it('names JWT_SECRET when it is blank', () => {
    expect(() => validateEnv({ JWT_SECRET: '  ' })).toThrow(/JWT_SECRET/);
  });

  it.each([[undefined], ['too-short']])('names INTERNAL_API_KEY when it is %p', (key) => {
    expect(() => validateEnv({ JWT_SECRET: 'x', DATABASE_URL: 'postgres://x', INTERNAL_API_KEY: key }))
      .toThrow(/INTERNAL_API_KEY/);
  });
});
