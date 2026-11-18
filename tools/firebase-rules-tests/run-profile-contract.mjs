import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';
import { spawn } from 'node:child_process';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, deleteField } from 'firebase/firestore';

// Test-only privileged writer. Never load credentials or accept arbitrary documents/fields.
if (process.env.FIRESTORE_EMULATOR_HOST !== '127.0.0.1:8086' ||
    process.env.FIREBASE_AUTH_EMULATOR_HOST !== '127.0.0.1:9096') {
  throw new Error('Only localhost demo emulators are permitted');
}
const nonce = randomUUID();
const env = await initializeTestEnvironment({ projectId: 'demo-havamania-audit',
  firestore: { host: '127.0.0.1', port: 8086 } });
const server = createServer(async (req, res) => {
  try {
    if (req.method !== 'POST' || req.url !== '/seed' || req.headers['x-emulator-test'] !== nonce) {
      res.writeHead(403).end(); return;
    }
    let body = '';
    for await (const chunk of req) {
      body += chunk;
      if (body.length > 2048) { res.writeHead(413).end(); return; }
    }
    const { uid, mode } = JSON.parse(body);
    if (typeof uid !== 'string' || !/^[a-zA-Z0-9_-]{1,128}$/.test(uid)) {
      res.writeHead(400).end(); return;
    }
    const modes = {
      canonical_true: { isPremium: true, premium: deleteField() },
      legacy_true: { isPremium: deleteField(), premium: true },
      agreed_false: { isPremium: false, premium: false },
      conflict: { isPremium: false, premium: true },
      malformed: { isPremium: 'invalid-test-value', premium: deleteField() }
    };
    if (!Object.hasOwn(modes, mode)) { res.writeHead(400).end(); return; }
    await env.withSecurityRulesDisabled(async context => {
      const ref = doc(context.firestore(), 'users', uid);
      await setDoc(ref, modes[mode], { merge: true });
      const verified = (await getDoc(ref)).data();
      console.log('Profile fixture:', mode, 'canonical=', verified.isPremium, 'legacy=', verified.premium);
    });
    res.writeHead(200, { 'Content-Type': 'application/json' }).end('{"ok":true}');
  } catch { res.writeHead(500).end(); }
});
try {
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(9396, '127.0.0.1', resolve);
  });
  const child = spawn('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass',
    '-File', 'run-android.ps1', '-ProfileContract'], {
    cwd: new URL('.', import.meta.url), stdio: 'inherit',
    env: { ...process.env, CITY_PROFILE_TEST_NONCE: nonce }
  });
  process.exitCode = await new Promise((resolve, reject) => {
    child.once('error', reject); child.once('exit', code => resolve(code ?? 1));
  });
} finally {
  await new Promise(resolve => server.close(resolve));
  await env.cleanup();
}