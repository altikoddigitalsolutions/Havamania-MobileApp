import net from 'node:net';
import http from 'node:http';
import { randomUUID } from 'node:crypto';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

if (process.env.FIRESTORE_EMULATOR_HOST !== '127.0.0.1:8086' ||
    process.env.FIREBASE_AUTH_EMULATOR_HOST !== '127.0.0.1:9096') {
  throw new Error('Local demo emulators are required');
}
const replyLoss = process.argv.includes('--reply-loss');
if (process.argv.slice(2).some(arg => arg !== '--reply-loss')) throw new Error('Unknown proxy mode');
const nonce = randomUUID();
const sockets = new Set();
let cut = false;
let destroyedSockets = 0;
let muted = false;
let droppedReplyBytes = 0;
const proxy = net.createServer(front => {
  if (cut) { front.destroy(); return; }
  const back = net.createConnection({ host: '127.0.0.1', port: 8086 });
  for (const socket of [front, back]) {
    sockets.add(socket);
    socket.on('close', () => sockets.delete(socket));
  }
  front.on('error', () => back.destroy());
  back.on('error', () => front.destroy());
  front.on('close', () => back.destroy());
  back.on('close', () => front.destroy());
  front.pipe(back);
  back.on('data', chunk => {
    if (muted) { droppedReplyBytes += chunk.length; return; }
    if (!front.write(chunk)) back.pause();
  });
  front.on('drain', () => back.resume());
});
const control = http.createServer((req, res) => {
  req.resume();
  if (req.method !== 'POST' || req.headers['x-emulator-test'] !== nonce) {
    res.writeHead(403); res.end(); return;
  }
  if (req.url === '/cut') {
    cut = true;
    destroyedSockets += sockets.size;
    for (const socket of sockets) socket.destroy();
  } else if (req.url === '/mute') {
    muted = true;
  } else if (req.url === '/restore') {
    cut = false;
    if (muted) {
      muted = false;
      destroyedSockets += sockets.size;
      for (const socket of sockets) socket.destroy();
    }
  } else if (req.url !== '/status') {
    res.writeHead(404); res.end(); return;
  }
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ cut, destroyedSockets, muted, droppedReplyBytes }));
});
function listen(server, port) {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(port, '127.0.0.1', resolve);
  });
}
try {
  await listen(proxy, 8087);
  await listen(control, 9296);
  process.exitCode = await new Promise((resolve, reject) => {
    const child = spawn('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
      fileURLToPath(new URL('run-android.ps1', import.meta.url)), replyLoss ? '-ReplyLoss' : '-TransportCut'], {
      stdio: 'inherit', env: { ...process.env, CITY_NETWORK_TEST_NONCE: nonce }
    });
    child.once('error', reject);
    child.once('exit', code => resolve(code ?? 1));
  });
} finally {
  for (const socket of sockets) socket.destroy();
  await Promise.all([proxy, control].map(server => new Promise(resolve => {
    if (!server.listening) resolve(); else server.close(resolve);
  })));
}
