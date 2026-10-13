import { INestApplication } from '@nestjs/common';
import { DocumentBuilder, OpenAPIObject, SwaggerModule } from '@nestjs/swagger';


// The name @ApiBearerAuth refers to; dev's controllers use 'jwt'
export const ACCESS_TOKEN_SCHEME = 'jwt';

// Swagger UI here, the raw document at DOCS_PATH + '-json'
export const DOCS_PATH = 'api/docs';

export function buildOpenApiDocument(app: INestApplication): OpenAPIObject {
  const config = new DocumentBuilder()
    .setTitle('Ribbit Auth Service')
    .setDescription(
      'Issues the tokens the trading API (Spring, port 8081) accepts.\n\n' +
      '1. Log in with `/auth/login` (clients) or `/auth/adminLogin` (admins) to get an `accessToken` and a `refreshToken`.\n' +
      '2. Send the access token to Spring as `Authorization: Bearer <accessToken>`. It lasts `JWT_EXPIRATION` seconds (30 minutes by default).\n' +
      '3. Trade the refresh token at `/auth/refresh` for a new pair, before or after the access token expires. Each refresh token works once.\n\n' +
      'A session ends `JWT_MAX_SESSION` seconds after login (8 hours by default), at logout, or as soon as a used refresh token is sent again.',
    )
    .setVersion('1.0.0')
    .addBearerAuth(
      { type: 'http', scheme: 'bearer', bearerFormat: 'JWT', description: 'The accessToken from login or refresh' },
      ACCESS_TOKEN_SCHEME,
    )
    .addTag('Auth', 'Authentication operations')
    .addTag('Health', 'Service health check')
    .build();
  return SwaggerModule.createDocument(app, config);
}


export function setupSwagger(app: INestApplication): void {
  SwaggerModule.setup(DOCS_PATH, app, buildOpenApiDocument(app), {
    // Keeps a pasted token across page reloads
    swaggerOptions: { persistAuthorization: true },
  });
}
