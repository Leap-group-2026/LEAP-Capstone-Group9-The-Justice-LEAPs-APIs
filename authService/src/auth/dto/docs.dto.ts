import { ApiProperty } from '@nestjs/swagger';

// Shapes used only to describe responses and the forwarded register body in Swagger; nothing validates against them

export class SessionTokensDto {
    @ApiProperty({ description: 'JWT to send to Spring as a Bearer token. Expires after JWT_EXPIRATION seconds.' })
    accessToken!: string;

    @ApiProperty({ description: 'Opaque, single-use token for POST /auth/refresh. Not a JWT, so Spring rejects it.' })
    refreshToken!: string;
}

export class MessageDto {
    @ApiProperty({ example: 'Logout successful' })
    message!: string;
}

export class AccessTokenClaimsDto {
    @ApiProperty({ description: 'user_id for a client, admin_id for an admin', example: '42' })
    sub!: string;

    @ApiProperty({ enum: ['client', 'admin'] })
    role!: string;

    @ApiProperty({ enum: ['access'] })
    type!: string;

    @ApiProperty({ description: 'Session id; every token from one login shares it', format: 'uuid' })
    sid!: string;

    @ApiProperty({ description: 'When the password was checked (Unix seconds); unchanged by refresh' })
    auth_time!: number;

    @ApiProperty({ description: 'Issued at (Unix seconds)' })
    iat!: number;

    @ApiProperty({ description: 'Expires at (Unix seconds)' })
    exp!: number;
}

export class VerifyResponseDto {
    @ApiProperty({ type: AccessTokenClaimsDto })
    user!: AccessTokenClaimsDto;
}

export class RegisterBodyDto {
    @ApiProperty({ example: 'London Haith' })
    name!: string;

    @ApiProperty({ example: 'london.haith@example.com' })
    email!: string;

    @ApiProperty({ format: 'date', example: '1990-01-01' })
    dateOfBirth!: string;

    @ApiProperty({ example: '1 Test St' })
    address!: string;

    @ApiProperty({ example: '123-45-6789' })
    ssn!: string;

    @ApiProperty({ example: 'Secret123!' })
    password!: string;
}
