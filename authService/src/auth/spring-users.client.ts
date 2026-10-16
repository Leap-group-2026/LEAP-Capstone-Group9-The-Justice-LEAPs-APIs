import { Injectable, BadGatewayException } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { AxiosResponse } from 'axios';
import { firstValueFrom } from 'rxjs';

@Injectable()
export class SpringUsersClient {
  constructor(private readonly http: HttpService) {}
  adminLogin(email: string, password:string){
    return this.send('post', '/admin/login', {email, password});
  }


  login(email: string, password: string) {
    return this.send('post', '/user/login', { email, password });
  }

  register(body: unknown) {
    return this.send('post', '/user', body);
  }

  checkSession(sid: string) {
    return this.send('post', `/internal/sessions/${encodeURIComponent(sid)}/check`);
  }

  revokeSession(sid: string, reason: string) {
    return this.send('post', `/internal/sessions/${encodeURIComponent(sid)}/revoke`, { reason });
  }

  private async send(method: 'post', url: string, body?: unknown): Promise<AxiosResponse> {
    try {
      return await firstValueFrom(this.http.request({ method, url, data: body }));
    } catch {
      throw new BadGatewayException('Account service is unavailable');
    }
  }
}
