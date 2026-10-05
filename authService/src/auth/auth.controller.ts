import { Controller, Post, Body, Get, UseGuards, Request } from '@nestjs/common';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './guards/jwt-auth.guard';

@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('login')
  async login(@Body() credentials: { email: string; password: string }) {
    // login stuff
    console.log('Login attempt:', credentials.email);
    return { message: 'Login endpoint - TODO' };
  }

  @Post('register')
  async register(@Body() userData: any) {
    // register stuff
    console.log('Registration attempt:', userData.email);
    return { message: 'Register endpoint - TODO' };
  }

  @Post('refresh')
  async refreshToken(@Body() body: { refreshToken: string }) {
    //refresh logic
    return { message: 'Refresh token endpoint - TODO' };
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
