import { Controller, Post, Body, Get, UseGuards, Request, HttpCode, Res } from '@nestjs/common';
import { Response } from 'express';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiBody } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { LoginDto } from './dto/login.dto';
import { SpringUsersClient } from './spring-users.client';

@ApiTags('Auth')
@Controller('auth')
export class AuthController {
  constructor(
    private authService: AuthService,
    private spring: SpringUsersClient,
  ) {}

  @Post('adminLogin')
  @HttpCode(200)
  @ApiOperation({ summary: 'Admin login' })
  @ApiBody({ type: LoginDto })
  @ApiResponse({ status: 200, description: 'Admin login successful', schema: { properties: { accessToken: { type: 'string' }, refreshToken: { type: 'string' } } } })
  @ApiResponse({ status: 401, description: 'Invalid credentials' })
  async adminLogin(@Body() dto: LoginDto) {
    return this.authService.adminLogin(dto);
  }

  @Post('login')
  @HttpCode(200)
  @ApiOperation({ summary: 'User login' })
  @ApiBody({ type: LoginDto })
  @ApiResponse({ status: 200, description: 'User login successful', schema: { properties: { accessToken: { type: 'string' }, refreshToken: { type: 'string' } } } })
  @ApiResponse({ status: 401, description: 'Invalid credentials' })
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @Post('register')
  @ApiOperation({ summary: 'User registration' })
  @ApiBody({ schema: { type: 'object', description: 'User registration data' } })
  @ApiResponse({ status: 201, description: 'User registered successfully' })
  @ApiResponse({ status: 400, description: 'Invalid registration data' })
  async register(@Body() body: Record<string, unknown>, @Res() res: Response) {
    const spring = await this.spring.register(body);
    res.status(spring.status).type(String(spring.headers['content-type'] ?? 'text/plain')).send(spring.data);
  }

  @Post('refresh')
  @HttpCode(200)
  @ApiOperation({ summary: 'Refresh access token' })
  @ApiBody({ schema: { type: 'object', properties: { refreshToken: { type: 'string' } }, required: ['refreshToken'] } })
  @ApiResponse({ status: 200, description: 'New access token generated', schema: { properties: { accessToken: { type: 'string' } } } })
  @ApiResponse({ status: 401, description: 'Invalid refresh token' })
  async refreshToken(@Body() body: { refreshToken: string }) {
    //refresh logic
    return { message: 'Refresh token endpoint - TODO' };
  }

  @Post('logout')
  @UseGuards(JwtAuthGuard)
  @HttpCode(200)
  @ApiBearerAuth('jwt')
  @ApiOperation({ summary: 'User logout' })
  @ApiResponse({ status: 200, description: 'Logout successful' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  async logout(@Request() req: any) {
    // logout logic
    return { message: 'Logout successful' };
  }

  @Get('verify')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('jwt')
  @ApiOperation({ summary: 'Verify JWT token validity' })
  @ApiResponse({ status: 200, description: 'Token is valid', schema: { properties: { user: { type: 'object' } } } })
  @ApiResponse({ status: 401, description: 'Unauthorized - Invalid or expired token' })
  async verifyToken(@Request() req: any) {
    // verification lofic
    return { user: req.user };
  }
}
