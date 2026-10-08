import { ApiProperty } from '@nestjs/swagger';
import { IsString, IsNotEmpty } from 'class-validator';

export class RefreshDto {
    @ApiProperty({ description: 'The refreshToken from the last login or refresh', example: 'mF3q9Yb0kq0J2pQ1v7rXcT4uZ8wA6sDdLhNeKgB5oPI' })
    @IsString() @IsNotEmpty() refreshToken!: string;
}
