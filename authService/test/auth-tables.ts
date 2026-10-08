import { Pool } from 'pg';

export const CLIENT_ID = 42;
export const ADMIN_ID = 7;


export async function resetAuthTables(db: Pool): Promise<void> {
  await db.query('TRUNCATE refresh_tokens, admin, user_info RESTART IDENTITY');
  await db.query(
    `INSERT INTO user_info (user_id, name, email, date_of_birth, address, ssn_hash, pass_hash)
     VALUES ($1, 'Jo Tester', 'jo@example.com', DATE '1990-01-01', '1 Test St', 'ssn', 'pass')`,
    [CLIENT_ID],
  );
  await db.query(
    `INSERT INTO admin (admin_id, email, pass_hash, role) VALUES ($1, 'admin@example.com', 'pass', 'ADMIN')`,
    [ADMIN_ID],
  );
}
