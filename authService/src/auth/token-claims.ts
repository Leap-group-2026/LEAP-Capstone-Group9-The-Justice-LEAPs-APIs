export interface AccessTokenClaims {
    sub: string;
    role: string;
    type: 'access';
    sid: string;
    auth_time: number;
    iat: number;
    exp: number;
}