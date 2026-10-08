import { execFileSync } from 'child_process';
import { readFileSync } from 'fs';
import { join } from 'path';
import { Client } from 'pg';

// A throwaway Postgres per e2e run, so token tests never touch the team's shared database.
// Port 0 lets Docker pick a free one; the URL reaches the tests through TEST_DATABASE_URL.
export default async function startTestPostgres() {
  const containerId = execFileSync('docker', [
    'run', '-d', '--rm',
    '-e', 'POSTGRES_PASSWORD=test',
    '-e', 'POSTGRES_DB=auth_test',
    '-p', '127.0.0.1::5432',
    'postgres:16-alpine',
  ], { encoding: 'utf8' }).trim();
  process.env.TEST_PG_CONTAINER = containerId;

  const hostPort = execFileSync('docker', ['port', containerId, '5432/tcp'], { encoding: 'utf8' }).trim().split('\n')[0];
  const url = `postgres://postgres:test@${hostPort}/auth_test`;
  const schema = readFileSync(join(__dirname, 'fixtures', 'auth-schema.sql'), 'utf8');

  // The image accepts TCP connections only once initialisation is finished, so retry until one succeeds
  const deadline = Date.now() + 30_000;
  for (;;) {
    const client = new Client({ connectionString: url });
    try {
      await client.connect();
      await client.query(schema);
      await client.end();
      break;
    } catch (err) {
      await client.end().catch(() => undefined);
      if (Date.now() > deadline) {
        execFileSync('docker', ['rm', '-f', containerId]);
        throw err;
      }
      await new Promise((resolve) => setTimeout(resolve, 500));
    }
  }

  process.env.TEST_DATABASE_URL = url;
}
