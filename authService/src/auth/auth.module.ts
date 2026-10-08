import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { JwtStrategy } from './strategies/jwt.strategy';
import { SpringUsersClient } from './spring-users.client';
import { EnvConfig } from '../config/env.validation';
import { DatabaseModule } from '../database/database.module';
import { RefreshTokenStore } from './refresh-token.store';

@Module({
  imports: [
    PassportModule,
    DatabaseModule,
    HttpModule.registerAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService<EnvConfig, true>) => ({
        baseURL: config.get('SPRINGBOOT_API_URL', { infer: true }),
        timeout: 5000,
        validateStatus: () => true,
      }),
    }),
    JwtModule.registerAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService<EnvConfig, true>) => ({
        secret: config.get('JWT_SECRET', { infer: true }),
        signOptions: { expiresIn: config.get('JWT_EXPIRATION', { infer: true }) },
      }),
    }),
  ],
  providers: [AuthService, JwtStrategy, SpringUsersClient, RefreshTokenStore],
  controllers: [AuthController],
  exports: [AuthService],
})
export class AuthModule {}
