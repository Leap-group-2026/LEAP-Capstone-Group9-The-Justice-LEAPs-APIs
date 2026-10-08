import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';

// No fake here: the real SpringUsersClient points at a port nothing listens on
describe('POST /auth/login with Spring down (N2)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    process.env.JWT_SECRET = 'test-secret';
    // Nothing listens here: these tests never touch Postgres, and must never reach a real database
    process.env.DATABASE_URL = 'postgres://test@127.0.0.1:1/none';
    process.env.SPRINGBOOT_API_URL = 'http://127.0.0.1:1';
    const { AppModule } = await import('../src/app.module');

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterAll(() => app.close());

  it('returns 502 and no token', async () => {
    const res = await request(app.getHttpServer())
      .post('/auth/login')
      .send({ email: 'jo@example.com', password: 'Secret' })
      .expect(502);

    expect(res.body.accessToken).toBeUndefined();
  });
});
