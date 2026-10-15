import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { createServer, IncomingHttpHeaders, Server } from 'http';
import { AddressInfo } from 'net';
import request from 'supertest';

const INTERNAL_API_KEY = 'test-only-internal-api-key-0123456789-abcdef';

// The real HTTP client against a fake Spring that records what it receives. Every other test replaces
// SpringUsersClient with a mock, so this is the only place the header Spring checks is actually sent.
describe('calls to Spring carry the internal API key', () => {
  let app: INestApplication;
  let fakeSpring: Server;
  const received: { url?: string; headers: IncomingHttpHeaders }[] = [];

  beforeAll(async () => {
    // Answers 401 like Spring does for bad credentials, so login never reaches the token database
    fakeSpring = createServer((req, res) => {
      received.push({ url: req.url, headers: req.headers });
      res.writeHead(401, { 'Content-Type': 'text/plain' }).end('Invalid email or password');
    });
    await new Promise<void>((resolve) => fakeSpring.listen(0, '127.0.0.1', resolve));

    process.env.JWT_SECRET = 'test-secret';
    process.env.INTERNAL_API_KEY = INTERNAL_API_KEY;
    process.env.SPRINGBOOT_API_URL = `http://127.0.0.1:${(fakeSpring.address() as AddressInfo).port}`;
    // Nothing listens here: these tests never touch Postgres
    process.env.DATABASE_URL = 'postgres://test@127.0.0.1:1/none';
    const { AppModule } = await import('../src/app.module');
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
    await new Promise((resolve) => fakeSpring.close(resolve));
  });

  beforeEach(() => { received.length = 0; });

  const credentials = { email: 'jo@example.com', password: 'Secret' };

  it.each([
    ['/auth/login', '/user/login'],
    ['/auth/adminLogin', '/admin/login'],
    ['/auth/register', '/user'],
  ])('%s sends X-Internal-Api-Key to Spring\'s %s', async (route, springPath) => {
    await request(app.getHttpServer()).post(route).send(credentials);

    expect(received).toHaveLength(1);
    expect(received[0].url).toBe(springPath);
    expect(received[0].headers['x-internal-api-key']).toBe(INTERNAL_API_KEY);
  });
});
