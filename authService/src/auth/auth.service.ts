import { BadGatewayException, Injectable, HttpException, Logger, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { LoginDto } from './dto/login.dto';
import { SpringUsersClient } from './spring-users.client';
import { ConfigService } from '@nestjs/config';
import { EnvConfig } from '../config/env.validation';
import { RefreshTokenStore, TokenOwner, generateRefreshToken } from './refresh-token.store';

export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
}

export interface AdminSessionTokens extends SessionTokens {
  role: string;
}

interface SpringSession {
  sid: string;
  expiresAt: Date;
}

type RefreshOutcome =
  | { ok: true; tokens: SessionTokens }
  | { ok: false; message: string; reusedSid?: string };

const INVALID_REFRESH_TOKEN = 'Invalid refresh token';
const SESSION_ENDED = 'Session has ended, please log in again';
const UUID_FORMAT = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;


@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  constructor(
    private jwtService: JwtService,
    private spring: SpringUsersClient,
    private configService: ConfigService<EnvConfig, true>,
    private refreshTokens: RefreshTokenStore,
  ) {}

  private signAccess(owner: TokenOwner, sessionId: string, authTime: Date, sessionExpiresAt: Date): Promise<string> {
    const now = Math.floor(Date.now() / 1000);
    const exp = Math.min(now + this.configService.get('JWT_EXPIRATION', { infer: true }),
      Math.floor(sessionExpiresAt.getTime() / 1000));
    if (exp <= now) {
      throw new UnauthorizedException('Session has expired, please log in again');
    }
    return this.jwtService.signAsync({
      sub: String(owner.id),
      role: owner.role,
      type: 'access',
      sid: sessionId,
      auth_time: Math.floor(authTime.getTime() / 1000),
      iat: now,
      exp,
    });
  }

  private async startSession(owner: TokenOwner, session: SpringSession): Promise<SessionTokens> {
    const authTime = new Date(Math.floor(Date.now() / 1000) * 1000);
    const refreshToken = generateRefreshToken();

    await this.refreshTokens.deleteExpired();
    await this.refreshTokens.create(refreshToken, { sessionId: session.sid, owner, authTime, expiresAt: session.expiresAt });
    return { accessToken: await this.signAccess(owner, session.sid, authTime, session.expiresAt), refreshToken };
  }

  private sessionFrom(data: { sid?: unknown; expiresAt?: unknown } | undefined): SpringSession {
    const expiresAt = typeof data?.expiresAt === 'string' ? new Date(data.expiresAt) : undefined;
    if (typeof data?.sid !== 'string' || !UUID_FORMAT.test(data.sid) || !expiresAt || Number.isNaN(expiresAt.getTime())) {
      throw new BadGatewayException('Account service is unavailable');
    }
    return { sid: data.sid, expiresAt };
  }

  async adminLogin({ email, password }: LoginDto): Promise<AdminSessionTokens> {
    const res = await this.spring.adminLogin(email, password);

    if (res.status === 200 && Number.isInteger(res.data?.id)) {
      const tokens = await this.startSession({ role: 'admin', id: res.data.id }, this.sessionFrom(res.data));
      return { ...tokens, role: res.data.role };
    }
    if (res.status === 401) {
      throw new UnauthorizedException('Invalid email or password');
    }
    if (res.status >= 400 && res.status < 500) {
      throw new HttpException(res.data, res.status);
    }
    throw new BadGatewayException('Account service is unavailable');
  }

  async login({ email, password }: LoginDto): Promise<SessionTokens> {
    const res = await this.spring.login(email, password);

    if (res.status === 200 && Number.isInteger(res.data?.id)) {
      return this.startSession({ role: 'client', id: res.data.id }, this.sessionFrom(res.data));
    }
    if (res.status === 401) {
      throw new UnauthorizedException('Invalid email or password');
    }
    if (res.status >= 400 && res.status < 500) {
      throw new HttpException(res.data, res.status);
    }
    throw new BadGatewayException('Account service is unavailable');
  }

  async checkSession(sid: string): Promise<Date> {
    const res = await this.spring.checkSession(sid);
    if (res.status === 200) {
      const expiresAt = new Date(res.data?.expiresAt);
      if (!Number.isNaN(expiresAt.getTime())) {
        return expiresAt;
      }
    }
    if (res.status === 401) {
      throw new UnauthorizedException(SESSION_ENDED);
    }
    throw new BadGatewayException('Account service is unavailable');
  }

  async refresh(rawRefreshToken: string): Promise<SessionTokens> {
    const known = await this.refreshTokens.find(rawRefreshToken);
    if (known === null) {
      throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
    }
    if (known.revokedAt !== null) {
      await this.refreshTokens.revokeSession(known.sessionId);
      await this.revokeInSpringBestEffort(known.sessionId, 'refresh_token_reuse');
      throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
    }
    if (known.expiresAt.getTime() <= Date.now()) {
      throw new UnauthorizedException('Session has expired, please log in again');
    }

    let sessionExpiresAt: Date;
    try {
      sessionExpiresAt = await this.checkSession(known.sessionId);
    } catch (err) {
      if (err instanceof UnauthorizedException) {
        await this.refreshTokens.revokeSession(known.sessionId);
      }
      throw err;
    }

    const outcome = await this.refreshTokens.transaction<RefreshOutcome>(async (tx) => {
      const current = await this.refreshTokens.findForUpdate(tx, rawRefreshToken);

      if (current === null) {
        return { ok: false, message: INVALID_REFRESH_TOKEN };
      }
      if (current.revokedAt !== null) {
        await this.refreshTokens.revokeSession(current.sessionId, tx);
        return { ok: false, message: INVALID_REFRESH_TOKEN, reusedSid: current.sessionId };
      }
      if (!(await this.refreshTokens.revoke(rawRefreshToken, tx))) {
        await this.refreshTokens.revokeSession(current.sessionId, tx);
        return { ok: false, message: INVALID_REFRESH_TOKEN, reusedSid: current.sessionId };
      }

      const refreshToken = generateRefreshToken();
      await this.refreshTokens.create(refreshToken, {
        sessionId: current.sessionId,
        owner: current.owner,
        authTime: current.authTime,
        expiresAt: sessionExpiresAt,
      }, tx);
      return {
        ok: true,
        tokens: {
          accessToken: await this.signAccess(current.owner, current.sessionId, current.authTime, sessionExpiresAt),
          refreshToken,
        },
      };
    });

    if (!outcome.ok) {
      if (outcome.reusedSid) {
        await this.revokeInSpringBestEffort(outcome.reusedSid, 'refresh_token_reuse');
      }
      throw new UnauthorizedException(outcome.message);
    }
    return outcome.tokens;
  }

  async logout(sessionId: string): Promise<void> {
    await this.refreshTokens.revokeSession(sessionId);
    const res = await this.spring.revokeSession(sessionId, 'logout');
    if (res.status < 200 || res.status >= 300) {
      throw new BadGatewayException('Account service is unavailable');
    }
  }

  private async revokeInSpringBestEffort(sessionId: string, reason: string): Promise<void> {
    try {
      const res = await this.spring.revokeSession(sessionId, reason);
      if (res.status < 200 || res.status >= 300) {
        this.logger.warn(`Spring answered ${res.status} to revoking a session after ${reason}`);
      }
    } catch {
      this.logger.warn(`Could not reach Spring to revoke a session after ${reason}`);
    }
  }
}
