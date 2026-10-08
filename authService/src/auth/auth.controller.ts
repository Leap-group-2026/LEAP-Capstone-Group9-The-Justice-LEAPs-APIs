import { Controller, Post, Body, Get, UseGuards, Request, HttpCode, Res } from '@nestjs/common';
import { Response } from 'express';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { LoginDto } from './dto/login.dto';
import { SpringUsersClient } from './spring-users.client';
import { AccessTokenClaims } from './token-claims';


@Controller('auth')
export class AuthController {
  constructor(
    private authService: AuthService,
    private spring: SpringUsersClient,
  ) {}

  @Post('adminLogin')
  @HttpCode(200)
  async adminLogin(@Body() dto: LoginDto) {
    return this.authService.adminLogin(dto);
  }

  @Post('login')
  @HttpCode(200)
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }


  @Post('register')
  async register(@Body() body: Record<string, unknown>, @Res() res: Response) {
    const spring = await this.spring.register(body);
    res.status(spring.status).type(String(spring.headers['content-type'] ?? 'text/plain')).send(spring.data);
  }

  @Post('refresh')
  @HttpCode(200)
  @UseGuards(JwtAuthGuard)
  refresh(@Request() req: { user: AccessTokenClaims }) {
    return this.authService.refresh(req.user);
  }

  @Post('logout')
  @UseGuards(JwtAuthGuard)
  async logout(@Request() req: any) {
    // logout logic
    return { message: 'Logout successful' };
  }

  @Get('verify')
  @UseGuards(JwtAuthGuard)
  async verifyToken(@Request() req: any) {
    // verification lofic
    return { user: req.user };
  }
}
