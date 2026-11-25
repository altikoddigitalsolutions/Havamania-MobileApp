import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const python = fileURLToPath(new URL('../../backend/venv/Scripts/python.exe', import.meta.url));
const child = spawn(python, ['backend-auth-session.py'], {
  cwd: fileURLToPath(new URL('.', import.meta.url)), stdio: 'inherit',
  env: { ...process.env, PYTHONIOENCODING: 'utf-8' }
});
child.once('error', error => { console.error(error.message); process.exitCode = 1; });
child.once('exit', code => { process.exitCode = code ?? 1; });
