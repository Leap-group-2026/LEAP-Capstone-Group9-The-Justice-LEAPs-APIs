import { BadGatewayException, Injectable, HttpException, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { LoginDto } from './dto/login.dto';
import { SpringUsersClient } from './spring-users.client';
import { ConfigService } from '@nestjs/config';
import { EnvConfig } from '../config/env.validation';
import { AccessTokenClaims } from './token-claims';


@Injectable()
export class AuthService {
  constructor(
    private jwtService: JwtService,
    private spring: SpringUsersClient,
    private configService: ConfigService<EnvConfig, true>,
  ) {}

  private signAccess(sub: string, role: string, authTime: number): Promise<string> {
    return this.jwtService.signAsync({ sub, role, type: 'access', auth_time: authTime });
  }

  async adminLogin({ email, password }: LoginDto): Promise<{ accessToken: string }> {
    const res = await this.spring.adminLogin(email, password);

    if (res.status === 200 && Number.isInteger(res.data?.id)) {
      const accessToken = await this.signAccess(String(res.data.id), 'admin', Math.floor(Date.now() / 1000));
      return { accessToken };
    }
    if (res.status === 401) {
      throw new UnauthorizedException('Invalid email or password');
    }
    if (res.status >= 400 && res.status < 500) {
      throw new HttpException(res.data, res.status);
    }
    throw new BadGatewayException('Account service is unavailable');
  }

  async login({ email, password }: LoginDto): Promise<{ accessToken: string }> {
    const res = await this.spring.login(email, password);

    if (res.status === 200 && Number.isInteger(res.data?.id)) {
      const accessToken = await this.signAccess(String(res.data.id), 'client', Math.floor(Date.now() / 1000));
      return { accessToken };
    }
    if (res.status === 401) {
      throw new UnauthorizedException('Invalid email or password'); 
    }
    if (res.status >= 400 && res.status < 500) {
      throw new HttpException(res.data, res.status); 
    }
    throw new BadGatewayException('Account service is unavailable'); 
  }

async refresh(claims: AccessTokenClaims): Promise<{ accessToken: string }> {
    const sessionAge = Math.floor(Date.now() / 1000) - claims.auth_time;
    if (sessionAge > this.configService.get('JWT_MAX_SESSION', { infer: true })) {
      throw new UnauthorizedException('Session has expired, please log in again');
    }
    return { accessToken: await this.signAccess(claims.sub, claims.role, claims.auth_time) };
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
