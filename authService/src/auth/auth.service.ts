import { BadGatewayException, Injectable, HttpException, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { LoginDto } from './dto/login.dto';
import { SpringUsersClient } from './spring-users.client';


@Injectable()
export class AuthService {
  constructor(
    private jwtService: JwtService,
    private spring: SpringUsersClient,
  ) {}

  async adminLogin({ email, password }: LoginDto): Promise<{ accessToken: string }> {
    const res = await this.spring.adminLogin(email, password);

    if (res.status === 200 && Number.isInteger(res.data?.id)) {
      const accessToken = await this.jwtService.signAsync({ sub: String(res.data.id), role: 'admin' });
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
      const accessToken = await this.jwtService.signAsync({ sub: String(res.data.id), role: 'client' });
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
