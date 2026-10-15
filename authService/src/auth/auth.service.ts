import { BadGatewayException, Injectable, HttpException, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { randomUUID } from 'crypto';
import { LoginDto } from './dto/login.dto';
import { SpringUsersClient } from './spring-users.client';
import { ConfigService } from '@nestjs/config';
import { EnvConfig } from '../config/env.validation';
import { RefreshTokenStore, TokenOwner, generateRefreshToken } from './refresh-token.store';

export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
}

type RefreshOutcome = { ok: true; tokens: SessionTokens } | { ok: false; message: string };

const INVALID_REFRESH_TOKEN = 'Invalid refresh token';


@Injectable()
export class AuthService {
  constructor(
    private jwtService: JwtService,
    private spring: SpringUsersClient,
    private configService: ConfigService<EnvConfig, true>,
    private refreshTokens: RefreshTokenStore,
  ) {}

  private signAccess(owner: TokenOwner, sessionId: string, authTime: Date): Promise<string> {
    return this.jwtService.signAsync({
      sub: String(owner.id),
      role: owner.role,
      type: 'access',
      sid: sessionId,
      auth_time: Math.floor(authTime.getTime() / 1000),
    });
  }

  private async startSession(owner: TokenOwner): Promise<SessionTokens> {
    const authTime = new Date(Math.floor(Date.now() / 1000) * 1000);
    const expiresAt = new Date(authTime.getTime() + this.configService.get('JWT_MAX_SESSION', { infer: true }) * 1000);
    const sessionId = randomUUID();
    const refreshToken = generateRefreshToken();

    await this.refreshTokens.deleteExpired();
    await this.refreshTokens.create(refreshToken, { sessionId, owner, authTime, expiresAt });
    return { accessToken: await this.signAccess(owner, sessionId, authTime), refreshToken };
  }

  async adminLogin({ email, password }: LoginDto): Promise<SessionTokens> {
    const res = await this.spring.adminLogin(email, password);

    if (res.status === 200 && Number.isInteger(res.data?.id)) {
      return this.startSession({ role: 'admin', id: res.data.id });
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
      return this.startSession({ role: 'client', id: res.data.id });
    }
    if (res.status === 401) {
      throw new UnauthorizedException('Invalid email or password');
    }
    if (res.status >= 400 && res.status < 500) {
      throw new HttpException(res.data, res.status);
    }
    throw new BadGatewayException('Account service is unavailable');
  }


  async refresh(rawRefreshToken: string): Promise<SessionTokens> {
    const outcome = await this.refreshTokens.transaction<RefreshOutcome>(async (tx) => {
      const current = await this.refreshTokens.findForUpdate(tx, rawRefreshToken);

      if (current === null) {
        return { ok: false, message: INVALID_REFRESH_TOKEN };
      }
      if (current.revokedAt !== null) {
        await this.refreshTokens.revokeSession(current.sessionId, tx);
        return { ok: false, message: INVALID_REFRESH_TOKEN };
      }
      if (current.expiresAt.getTime() <= Date.now()) {
        return { ok: false, message: 'Session has expired, please log in again' };
      }

      if (!(await this.refreshTokens.revoke(rawRefreshToken, tx))) {
        await this.refreshTokens.revokeSession(current.sessionId, tx);
        return { ok: false, message: INVALID_REFRESH_TOKEN };
      }

      const refreshToken = generateRefreshToken();
      await this.refreshTokens.create(refreshToken, {
        sessionId: current.sessionId,
        owner: current.owner,
        authTime: current.authTime,
        expiresAt: current.expiresAt,
      }, tx);
      return {
        ok: true,
        tokens: { accessToken: await this.signAccess(current.owner, current.sessionId, current.authTime), refreshToken },
      };
    });

    if (!outcome.ok) {
      throw new UnauthorizedException(outcome.message);
    }
    return outcome.tokens;
  }

  async logout(sessionId: string): Promise<void> {
    await this.refreshTokens.revokeSession(sessionId);
  }


  async generateToken(payload: any): Promise<string> {
    return this.jwtService.sign(payload);
  }

  async validateToken(token: string): Promise<any> {
    try {
      return this.jwtService.verify(token);
    } catch (error) {
      return null;
    }
  }

}
