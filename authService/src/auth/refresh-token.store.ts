import { Inject, Injectable } from '@nestjs/common';
import { createHash, randomBytes } from 'crypto';
import { Pool, PoolClient } from 'pg';
import { PG_POOL } from '../database/database.module';

export type TokenOwner = { role: 'client' | 'admin'; id: number };

export interface NewRefreshToken {
  sessionId: string;
  owner: TokenOwner;
  authTime: Date;
  expiresAt: Date;
}

export interface RefreshTokenRecord extends NewRefreshToken {
  revokedAt: Date | null;
}

type Queryable = Pool | PoolClient;

export function generateRefreshToken(): string {
  return randomBytes(32).toString('base64url');
}


export function hashRefreshToken(rawToken: string): string {
  return createHash('sha256').update(rawToken).digest('hex');
}


@Injectable()
export class RefreshTokenStore {
  constructor(@Inject(PG_POOL) private readonly pool: Pool) {}

  async transaction<T>(work: (tx: PoolClient) => Promise<T>): Promise<T> {
    const client = await this.pool.connect();
    let broken = false;
    try {
      await client.query('BEGIN');
      const result = await work(client);
      await client.query('COMMIT');
      return result;
    } catch (err) {
      await client.query('ROLLBACK').catch(() => { broken = true; });
      throw err;
    } finally {
      client.release(broken);
    }
  }

  async create(rawToken: string, token: NewRefreshToken, db: Queryable = this.pool): Promise<void> {
    await db.query(
      `INSERT INTO refresh_tokens (token_hash, session_id, user_id, admin_id, auth_time, expires_at)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [
        hashRefreshToken(rawToken),
        token.sessionId,
        token.owner.role === 'client' ? token.owner.id : null,
        token.owner.role === 'admin' ? token.owner.id : null,
        token.authTime,
        token.expiresAt,
      ],
    );
  }


  async findForUpdate(tx: PoolClient, rawToken: string): Promise<RefreshTokenRecord | null> {
    const { rows } = await tx.query(
      `SELECT session_id, user_id, admin_id, auth_time, expires_at, revoked_at
       FROM refresh_tokens WHERE token_hash = $1 FOR UPDATE`,
      [hashRefreshToken(rawToken)],
    );
    if (rows.length === 0) return null;

    const row = rows[0];
    return {
      sessionId: row.session_id,
      owner: row.admin_id !== null ? { role: 'admin', id: row.admin_id } : { role: 'client', id: row.user_id },
      authTime: row.auth_time,
      expiresAt: row.expires_at,
      revokedAt: row.revoked_at,
    };
  }


  async revoke(rawToken: string, db: Queryable = this.pool): Promise<boolean> {
    const { rowCount } = await db.query(
      'UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1 AND revoked_at IS NULL',
      [hashRefreshToken(rawToken)],
    );
    return rowCount === 1;
  }



  async deleteExpired(db: Queryable = this.pool): Promise<number> {
    const { rowCount } = await db.query('DELETE FROM refresh_tokens WHERE expires_at <= now()');
    return rowCount ?? 0;
  }

  async revokeSession(sessionId: string, db: Queryable = this.pool): Promise<number> {
    const { rowCount } = await db.query(
      'UPDATE refresh_tokens SET revoked_at = now() WHERE session_id = $1 AND revoked_at IS NULL',
      [sessionId],
    );
    return rowCount ?? 0;
  }
}
