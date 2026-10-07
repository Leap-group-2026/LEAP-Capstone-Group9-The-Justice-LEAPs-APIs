export interface AccessTokenClaims {
    sub: string;
    role: string;
    type: 'access';
    auth_time: number;
    iat: number;
    exp: number;
}