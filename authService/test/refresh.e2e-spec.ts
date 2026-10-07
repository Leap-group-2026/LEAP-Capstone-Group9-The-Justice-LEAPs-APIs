import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import request from 'supertest';
import { SpringUsersClient } from '../src/auth/spring-users.client';

const spring = { login: jest.fn(), register: jest.fn() };
const now = () => Math.floor(Date.now() / 1000);

describe('POST /auth/refresh', () => {
  let app: INestApplication;
  let jwt: JwtService;

  beforeAll(async () => {
    process.env.JWT_SECRET = 'test-secret';
    process.env.JWT_EXPIRATION = '1800';
    const { AppModule } = await import('../src/app.module');
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(SpringUsersClient).useValue(spring)
      .compile();
    app = moduleRef.createNestApplication();
    await app.init();
    jwt = app.get(JwtService);
  });

  afterAll(() => app.close());


  const tokenIssued = (minutesAgo: number, extra: object = {}) => {
    const iat = now() - minutesAgo * 60;
    return jwt.sign({ sub: '42', role: 'client', type: 'access', auth_time: iat, iat, ...extra });
  };
  const refresh = (token: string) =>
    request(app.getHttpServer()).post('/auth/refresh').set('Authorization', `Bearer ${token}`);

  it('slides a token issued 10 minutes ago to a new 30-minute token', async () => {
    const old = tokenIssued(10);

    const res = await refresh(old).expect(200);

    const before = jwt.decode(old);
    const after = jwt.verify(res.body.accessToken);
    expect(after.exp - after.iat).toBe(1800);
    expect(after.exp).toBeGreaterThan(before.exp);
    expect([after.sub, after.role, after.type, after.auth_time])
      .toEqual([before.sub, before.role, 'access', before.auth_time]);
    expect(spring.login).not.toHaveBeenCalled();
  });

  it('rejects a token issued 31 minutes ago (idle timeout)', async () => {
    const res = await refresh(tokenIssued(31)).expect(401);

    expect(res.body.accessToken).toBeUndefined();
  });

  it('rejects a token with a tampered signature', async () => {
    await refresh(tokenIssued(1).slice(0, -1) + 'X').expect(401);
  });

  it.each([['refresh'], [undefined]])('rejects type %s on /auth/refresh and /auth/verify', async (type) => {
    const token = tokenIssued(1, { type });

    await refresh(token).expect(401);
    await request(app.getHttpServer()).get('/auth/verify').set('Authorization', `Bearer ${token}`).expect(401);
  });

  it('ends a session older than 8 hours even if the token is fresh', async () => {
    await refresh(tokenIssued(1, { auth_time: now() - 9 * 3600 })).expect(401);
  });

  it('returns 401 with no Authorization header', async () => {
    await request(app.getHttpServer()).post('/auth/refresh').expect(401);
  });
});
