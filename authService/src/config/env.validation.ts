export interface EnvConfig {
  PORT: number;
  JWT_SECRET: string;
  JWT_EXPIRATION: number;
  SPRINGBOOT_API_URL: string;
  CORS_ORIGIN: string;
  JWT_MAX_SESSION: number;
}


export function validateEnv(env: Record<string, unknown>): EnvConfig {
  const secret = env.JWT_SECRET;
  if (typeof secret !== 'string' || secret.trim() === '') {
    throw new Error('JWT_SECRET is not set. Add it to authService/.env (see .env.example).');
  }

  return {
    PORT: toPositiveInt(env, 'PORT', 3001),
    JWT_SECRET: secret,
    JWT_EXPIRATION: toPositiveInt(env, 'JWT_EXPIRATION', 1800),
    SPRINGBOOT_API_URL: toText(env, 'SPRINGBOOT_API_URL', 'http://localhost:8081'),
    CORS_ORIGIN: toText(env, 'CORS_ORIGIN', 'http://localhost:4200'),
    JWT_MAX_SESSION: toPositiveInt(env, 'JWT_MAX_SESSION', 28800),
  };
}

function toPositiveInt(env: Record<string, unknown>, name: string, fallback: number): number {
  const raw = env[name];
  if (raw === undefined || raw === '') return fallback;
  const value = Number(raw);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive whole number, got "${raw}".`);
  }
  return value;
}

function toText(env: Record<string, unknown>, name: string, fallback: string): string {
  const raw = env[name];
  return typeof raw === 'string' && raw !== '' ? raw : fallback;
}
