import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';
import { spawn } from 'node:child_process';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc, deleteDoc, getDoc } from 'firebase/firestore';

if (process.env.FIRESTORE_EMULATOR_HOST !== '127.0.0.1:8086' ||
    process.env.FIREBASE_AUTH_EMULATOR_HOST !== '127.0.0.1:9096') throw new Error('Only loopback demo emulators');
const nonce = randomUUID();
const restartRejection = process.argv.includes('--restart-rejection');
const authSession = process.argv.includes('--auth-session');
if (restartRejection && authSession) throw new Error('Select one fixture mode');
const env = await initializeTestEnvironment({ projectId: 'demo-havamania-audit',
  firestore: { host: '127.0.0.1', port: 8086 } });
const blocked = new Set();
const disabled = new Set();
async function setDisabled(uid, value) {
  const reply = await fetch('http://127.0.0.1:9096/identitytoolkit.googleapis.com/v1/projects/demo-havamania-audit/accounts:update', {
    method: 'POST', headers: { Authorization: 'Bearer owner', 'Content-Type': 'application/json' },
    body: JSON.stringify({ localId: uid, disableUser: value }), signal: AbortSignal.timeout(3000)
  });
  await reply.text();
  if (!reply.ok) throw new Error('Local Auth fixture rejected update');
}
const server = createServer(async (req, res) => {
  try {
    if (req.method !== 'POST' || !(authSession ? ['/auth-disable', '/auth-enable', '/auth-revoke', '/profile'] : ['/block', '/unblock']).includes(req.url) || req.headers['x-emulator-test'] !== nonce) {
      res.writeHead(403).end(); return;
    }
    let body = '';
    for await (const chunk of req) { body += chunk; if (body.length > 512) { res.writeHead(413).end(); return; } }
    const { uid, issuedAt } = JSON.parse(body);
    if (typeof uid !== 'string' || !/^[a-zA-Z0-9_-]{1,128}$/.test(uid)) { res.writeHead(400).end(); return; }
    if (authSession) {
      if (req.url === '/profile') {
        let fields;
        await env.withSecurityRulesDisabled(async context => { fields = (await getDoc(doc(context.firestore(), 'users', uid))).data(); });
        res.writeHead(200, { 'Content-Type': 'application/json' }).end(JSON.stringify({ name: fields?.name, bio: fields?.bio }));
        return;
      }
      if (req.url === '/auth-revoke') {
        if (!Number.isSafeInteger(issuedAt) || Math.abs(Date.now() / 1000 - issuedAt) > 10) {
          res.writeHead(400).end(); return;
        }
        const deadline = Date.now() + 3000;
        while (Math.floor(Date.now() / 1000) <= issuedAt) {
          if (Date.now() > deadline) throw new Error('Local revocation clock boundary timed out');
          await new Promise(resolve => setTimeout(resolve, 25));
        }
        const validSince = String(Math.floor(Date.now() / 1000));
        const reply = await fetch('http://127.0.0.1:9096/identitytoolkit.googleapis.com/v1/projects/demo-havamania-audit/accounts:update', {
          method: 'POST', headers: { Authorization: 'Bearer owner', 'Content-Type': 'application/json' },
          body: JSON.stringify({ localId: uid, validSince }), signal: AbortSignal.timeout(3000)
        });
        await reply.text();
        if (!reply.ok) throw new Error('Local Auth revocation rejected');
        res.writeHead(200).end(JSON.stringify({ validSince })); return;
      }
      const value = req.url === '/auth-disable';
      await setDisabled(uid, value);
      if (value) disabled.add(uid); else disabled.delete(uid);
      res.writeHead(200).end('{"ok":true}'); return;
    }
    await env.withSecurityRulesDisabled(async context => {
      const ref = doc(context.firestore(), 'account_deletions', uid);
      if (req.url === '/block') { await setDoc(ref, { reason: 'local-offline-profile-test' }); blocked.add(uid); }
      else { await deleteDoc(ref); blocked.delete(uid); }
    });
    res.writeHead(200).end('{"ok":true}');
  } catch { res.writeHead(500).end(); }
});
try {
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(9397, '127.0.0.1', resolve); });
  const child = spawn('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', 'run-android.ps1', authSession ? '-AuthSessionValidation' : restartRejection ? '-PendingProfileRejectRestart' : '-ProfileOffline'], {
    cwd: new URL('.', import.meta.url), stdio: 'inherit', env: { ...process.env, PROFILE_OFFLINE_TEST_NONCE: nonce }
  });
  process.exitCode = await new Promise((resolve, reject) => { child.once('error', reject); child.once('exit', code => resolve(code ?? 1)); });
} finally {
  await new Promise(resolve => server.close(resolve));
  await env.withSecurityRulesDisabled(async context => {
    for (const uid of blocked) await deleteDoc(doc(context.firestore(), 'account_deletions', uid));
  });
  for (const uid of disabled) await setDisabled(uid, false);
  await env.cleanup();
}
