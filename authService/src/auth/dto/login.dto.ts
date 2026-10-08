import { ApiProperty } from '@nestjs/swagger';
import { IsString, IsNotEmpty } from 'class-validator';

export class LoginDto {
    @ApiProperty({ example: 'jo@example.com' })
    @IsString() @IsNotEmpty() email!: string;

    @ApiProperty({ example: 'Secret123!' })
    @IsString() @IsNotEmpty() password!: string;
}
