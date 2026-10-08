import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { setupSwagger, ACCESS_TOKEN_SCHEME } from '../src/swagger';

// method -> path for every route the auth service serves; a new route must be added here, which is the point
const ENDPOINTS: [string, string][] = [
  ['post', '/auth/login'],
  ['post', '/auth/adminLogin'],
  ['post', '/auth/register'],
  ['post', '/auth/refresh'],
  ['post', '/auth/logout'],
  ['get', '/auth/verify'],
  ['get', '/health'],
];
const NEEDS_ACCESS_TOKEN = ['post /auth/logout', 'get /auth/verify'];

describe('API docs (GET /docs-json)', () => {
  let app: INestApplication;
  let doc: any;

  beforeAll(async () => {
    process.env.JWT_SECRET = 'test-secret';
    process.env.INTERNAL_API_KEY = 'test-only-internal-api-key-0123456789-abcdef';
    // Nothing listens here: building the docs never touches Postgres
    process.env.DATABASE_URL = 'postgres://test@127.0.0.1:1/none';
    const { AppModule } = await import('../src/app.module');
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = moduleRef.createNestApplication();
    setupSwagger(app);
    await app.init();
    doc = (await request(app.getHttpServer()).get('/docs-json').expect(200)).body;
  });

  afterAll(() => app.close());

  const operation = (method: string, path: string) => doc.paths[path]?.[method];
  const schemaOf = (ref: string) => doc.components.schemas[ref.split('/').pop()!];

  it('documents every route, and nothing else', () => {
    const documented = Object.entries(doc.paths).flatMap(([path, ops]) =>
      Object.keys(ops as object).map((method) => `${method} ${path}`));

    expect(documented.sort()).toEqual(ENDPOINTS.map(([m, p]) => `${m} ${p}`).sort());
  });

  it('defines the Bearer scheme that the Authorize button fills in', () => {
    expect(doc.components.securitySchemes[ACCESS_TOKEN_SCHEME]).toMatchObject({ type: 'http', scheme: 'bearer' });
  });

  it('asks for an access token on exactly the routes that check one', () => {
    const secured = ENDPOINTS
      .filter(([m, p]) => operation(m, p).security?.some((s: object) => ACCESS_TOKEN_SCHEME in s))
      .map(([m, p]) => `${m} ${p}`);

    expect(secured.sort()).toEqual([...NEEDS_ACCESS_TOKEN].sort());
  });

  it('documents refresh as taking a required refreshToken in the body, with 200, 400 and 401', () => {
    const refresh = operation('post', '/auth/refresh');
    const body = schemaOf(refresh.requestBody.content['application/json'].schema.$ref);

    expect(body.required).toEqual(['refreshToken']);
    expect(Object.keys(refresh.responses).sort()).toEqual(['200', '400', '401']);
  });

  it.each([['/auth/login'], ['/auth/adminLogin'], ['/auth/refresh']])('documents %s as returning both tokens', (path) => {
    const ok = schemaOf(operation('post', path).responses['200'].content['application/json'].schema.$ref);

    expect(Object.keys(ok.properties).sort()).toEqual(['accessToken', 'refreshToken']);
  });
});
