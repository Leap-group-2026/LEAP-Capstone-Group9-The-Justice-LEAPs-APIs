import { Controller, Post, Body, Get, UseGuards, Request, HttpCode, Res } from '@nestjs/common';
import { Response } from 'express';
import {
  ApiBadGatewayResponse, ApiBadRequestResponse, ApiBearerAuth, ApiBody, ApiOkResponse, ApiOperation, ApiResponse,
  ApiTags, ApiUnauthorizedResponse,
} from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { LoginDto } from './dto/login.dto';
import { RefreshDto } from './dto/refresh.dto';
import { SpringUsersClient } from './spring-users.client';
import { AccessTokenClaims } from './token-claims';
import { MessageDto, RegisterBodyDto, SessionTokensDto, VerifyResponseDto } from './dto/docs.dto';
import { ACCESS_TOKEN_SCHEME } from '../swagger';

@ApiTags('Auth')
@Controller('auth')
export class AuthController {
  constructor(
    private authService: AuthService,
    private spring: SpringUsersClient,
  ) {}

  @Post('adminLogin')
  @HttpCode(200)
  @ApiOperation({ summary: 'Admin login', description: 'Checks the password with Spring and starts a new session for an admin (role admin).' })
  @ApiBody({ type: LoginDto })
  @ApiOkResponse({ type: SessionTokensDto, description: 'Admin login successful' })
  @ApiBadRequestResponse({ description: 'email or password missing or blank' })
  @ApiUnauthorizedResponse({ description: 'Invalid email or password (same message for both, on purpose)' })
  @ApiBadGatewayResponse({ description: 'Spring is unreachable' })
  async adminLogin(@Body() dto: LoginDto) {
    return this.authService.adminLogin(dto);
  }

  @Post('login')
  @HttpCode(200)
  @ApiOperation({ summary: 'User login', description: 'Checks the password with Spring and starts a new session for a client (role client). Each login is its own session.' })
  @ApiBody({ type: LoginDto })
  @ApiOkResponse({ type: SessionTokensDto, description: 'User login successful' })
  @ApiBadRequestResponse({ description: 'email or password missing or blank' })
  @ApiUnauthorizedResponse({ description: 'Invalid email or password (same message for both, on purpose)' })
  @ApiBadGatewayResponse({ description: 'Spring is unreachable' })
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @Post('register')
  @ApiOperation({ summary: 'User registration', description: "Forwarded to Spring's POST /user unchanged; Spring's status and body come back as they are." })
  @ApiBody({ type: RegisterBodyDto })
  @ApiResponse({ status: 201, description: "User registered successfully (Spring's response body)" })
  @ApiBadRequestResponse({ description: "Invalid registration data, e.g. a weak password; Spring's message is passed through" })
  @ApiBadGatewayResponse({ description: 'Spring is unreachable' })
  async register(@Body() body: Record<string, unknown>, @Res() res: Response) {
    const spring = await this.spring.register(body);
    res.status(spring.status).type(String(spring.headers['content-type'] ?? 'text/plain')).send(spring.data);
  }

  // No JwtAuthGuard: the refresh token is the credential here, so an expired access token is fine
  @Post('refresh')
  @HttpCode(200)
  @ApiOperation({
    summary: 'Refresh access token',
    description: 'Swaps a refresh token for a new pair. No Authorization header: works whether or not the access token ' +
      'has expired. The refresh token sent is used up; keep the new one. Sending an already-used token again ends the ' +
      'whole session, since it means a copy exists somewhere. Two refreshes with the same token at once therefore log the user out.',
  })
  @ApiOkResponse({ type: SessionTokensDto, description: 'New access token and refresh token; same session, same 8-hour limit' })
  @ApiBadRequestResponse({ description: 'refreshToken missing, blank or not a string' })
  @ApiUnauthorizedResponse({ description: '"Invalid refresh token" (unknown, used or logged out) or "Session has expired, please log in again"' })
  refresh(@Body() dto: RefreshDto) {
    return this.authService.refresh(dto.refreshToken);
  }

  @Post('logout')
  @UseGuards(JwtAuthGuard)
  @HttpCode(200)
  @ApiBearerAuth(ACCESS_TOKEN_SCHEME)
  @ApiOperation({
    summary: 'User logout',
    description: 'Revokes every refresh token from this login; other logins stay signed in. The access token itself ' +
      'keeps working at Spring until it expires.',
  })
  @ApiOkResponse({ type: MessageDto, description: 'Logout successful' })
  @ApiUnauthorizedResponse({ description: 'Missing, expired or invalid access token' })
  async logout(@Request() req: { user: AccessTokenClaims }) {
    await this.authService.logout(req.user.sid);
    return { message: 'Logout successful' };
  }

  @Get('verify')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth(ACCESS_TOKEN_SCHEME)
  @ApiOperation({ summary: 'Verify JWT token validity', description: 'Returns the claims of a valid access token.' })
  @ApiOkResponse({ type: VerifyResponseDto, description: 'Token is valid' })
  @ApiUnauthorizedResponse({ description: 'Unauthorized - invalid or expired token' })
  async verifyToken(@Request() req: any) {
    return { user: req.user };
  }
}
