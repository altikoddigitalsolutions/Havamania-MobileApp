import { after, before, beforeEach, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { ref, uploadBytes, getBytes, deleteObject } from 'firebase/storage';

let env;
before(async () => {
  // Never fall back to production, even if invoked without emulators:exec.
  if (process.env.FIRESTORE_EMULATOR_HOST !== '127.0.0.1:8086' ||
      process.env.FIREBASE_STORAGE_EMULATOR_HOST !== '127.0.0.1:9196') {
    throw new Error('Expected local Firestore/Storage emulators; refusing to run');
  }
  env = await initializeTestEnvironment({
    projectId: 'demo-havamania-audit',
    firestore: { host: '127.0.0.1', port: 8086,
      rules: await readFile(new URL('../../mobile/firebase-rules/firestore.rules', import.meta.url), 'utf8') },
    storage: { host: '127.0.0.1', port: 9196,
      rules: await readFile(new URL('../../mobile/firebase-rules/storage.rules', import.meta.url), 'utf8') }
  });
});
beforeEach(async () => { await env.clearFirestore(); await env.clearStorage(); });
after(async () => { await env?.cleanup(); });

test('owner can write profile and nested data; another user cannot read/write', async () => {
  const owner = env.authenticatedContext('owner').firestore();
  const other = env.authenticatedContext('other').firestore();
  for (const path of ['users/owner', 'users/owner/trips/trip', 'users/owner/unknown/doc/nested/child']) {
    await assertSucceeds(setDoc(doc(owner, path), {value: 'private'}));
    await assertFails(getDoc(doc(other, path)));
    await assertFails(setDoc(doc(other, path), {value: 'changed'}));
  }
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'users/owner')));
});

test('deletion marker blocks the same stale authenticated context at every depth', async () => {
  const owner = env.authenticatedContext('owner').firestore();
  await assertSucceeds(setDoc(doc(owner, 'users/owner'), {value: 'before'}));
  await assertFails(setDoc(doc(owner, 'account_deletions/owner'), {blocked: false}));
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'account_deletions/owner'), {blocked: true});
  });
  for (const path of ['users/owner', 'users/owner/trips/trip', 'users/owner/unknown/doc/nested/child']) {
    await assertFails(setDoc(doc(owner, path), {value: 'recreated'}));
    await assertFails(getDoc(doc(owner, path)));
  }
});

test('storage accepts owner image and delete, rejects other users and invalid uploads', async () => {
  const owner = env.authenticatedContext('owner').storage();
  const path = 'profile-images/owner/avatar.jpg';
  await assertSucceeds(uploadBytes(ref(owner, path), new Uint8Array([1, 2, 3]), {contentType: 'image/jpeg'}));
  await assertSucceeds(getBytes(ref(owner, path)));
  await assertFails(getBytes(ref(env.authenticatedContext('other').storage(), path)));
  await assertFails(uploadBytes(ref(owner, path), new Uint8Array([1]), {contentType: 'text/html'}));
  await assertFails(uploadBytes(ref(owner, path), new Uint8Array(5 * 1024 * 1024), {contentType: 'image/jpeg'}));
  await assertSucceeds(deleteObject(ref(owner, path)));
});

test('storage rejects new uploads after Firestore deletion marker', async () => {
  const owner = env.authenticatedContext('owner').storage();
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'account_deletions/owner'), {blocked: true});
  });
  await assertFails(uploadBytes(ref(owner, 'profile-images/owner/avatar.jpg'),
    new Uint8Array([1]), {contentType: 'image/jpeg'}));
});
