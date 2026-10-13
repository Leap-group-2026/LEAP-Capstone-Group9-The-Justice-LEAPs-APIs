import { Controller, Get } from '@nestjs/common';
import { ApiOkResponse, ApiOperation, ApiTags } from '@nestjs/swagger';
import { AppService } from './app.service';

@ApiTags('Health')
@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  @Get()
  @ApiOperation({ summary: 'Health check endpoint' })
  @ApiOkResponse({ description: 'Service is running' })
  getHello(): string {
    return this.appService.getHello();
  }

  @Get('health')
  @ApiOperation({ summary: 'Get service health status' })
  @ApiOkResponse({ description: 'Service health status', schema: { properties: { status: { type: 'string', example: 'OK' } } } })
  health(): { status: string } {
    return { status: 'OK' };
  }
}
