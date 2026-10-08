import { Test } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { validateEnv } from '../config/env.validation';

describe('JWT signing', () => {
  let authService: import('./auth.service').AuthService;
  let jwtService: JwtService;

  beforeAll(async () => {
    process.env.JWT_SECRET = 'test-secret';
    process.env.JWT_EXPIRATION = '1800';
    const { AppModule } = await import('../app.module');
    const { AuthService } = await import('./auth.service');

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    authService = moduleRef.get(AuthService);
    jwtService = moduleRef.get(JwtService);
  });

  it('issues tokens that expire JWT_EXPIRATION seconds after they are issued', async () => {
    const token = await authService.generateToken({ sub: 1 });
    const { iat, exp } = jwtService.decode(token);

    expect(exp - iat).toBe(1800);
  });
});

describe('validateEnv', () => {
  it('names JWT_SECRET when it is missing', () => {
    expect(() => validateEnv({ JWT_EXPIRATION: '1800' })).toThrow(/JWT_SECRET/);
  });

  it('names JWT_SECRET when it is blank', () => {
    expect(() => validateEnv({ JWT_SECRET: '  ' })).toThrow(/JWT_SECRET/);
  });
});
