import { execFileSync } from 'child_process';

export default async function stopTestPostgres() {
  if (process.env.TEST_PG_CONTAINER) {
    execFileSync('docker', ['rm', '-f', process.env.TEST_PG_CONTAINER]);
  }
}
