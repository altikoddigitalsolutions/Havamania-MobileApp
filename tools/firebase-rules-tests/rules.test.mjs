import { after, before, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, getDocs, collection, deleteField, deleteDoc, runTransaction } from 'firebase/firestore';
import { ref, uploadBytes, getBytes, deleteObject } from 'firebase/storage';

let env;
test('editorial guides are readable individually but clients cannot list or publish', async () => {
  const guest = env.unauthenticatedContext().firestore();
  const user = env.authenticatedContext('owner').firestore();
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'travel_guides/reviewed'), { payload: '{}' });
  });
  await assertSucceeds(getDoc(doc(guest, 'travel_guides/reviewed')));
  await assertSucceeds(getDoc(doc(user, 'travel_guides/reviewed')));
  await assertFails(getDocs(collection(user, 'travel_guides')));
  await assertFails(setDoc(doc(user, 'travel_guides/reviewed'), { payload: 'changed' }));
  await assertFails(setDoc(doc(guest, 'travel_guides/new'), { payload: 'changed' }));
});
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


const defaultLocationFields = () => ({ defaultCity: 'Paris', defaultCityLocation: {
  version: 1, id: 1, name: 'Paris', latitude: 48.8, longitude: 2.3,
  country: 'France', country_code: 'FR', admin1: null, admin2: null, admin3: null
}});

test('default location companion round trips and merge preserves unrelated profile fields', async () => {
  const db = env.authenticatedContext('owner').firestore();
  const profile = doc(db, 'users/owner');
  await setDoc(profile, { displayName: 'Preserved', defaultCity: 'Ankara', interests: ['walking'] });
  const fields = defaultLocationFields();
  await assertSucceeds(setDoc(profile, fields, { merge: true }));
  const saved = (await getDoc(profile)).data();
  assert.deepEqual(saved, { displayName: 'Preserved', interests: ['walking'], ...fields });
  // A legacy reader still sees its expected name field.
  assert.equal(saved.defaultCity, 'Paris');
});

test('default location rejects other account and guest reads and writes', async () => {
  const owner = env.authenticatedContext('owner').firestore();
  const fields = defaultLocationFields();
  await setDoc(doc(owner, 'users/owner'), fields);
  for (const db of [env.authenticatedContext('other').firestore(), env.unauthenticatedContext().firestore()]) {
    await assertFails(getDoc(doc(db, 'users/owner')));
    await assertFails(setDoc(doc(db, 'users/owner'), fields, { merge: true }));
  }
  assert.deepEqual((await getDoc(doc(owner, 'users/owner'))).data(), fields);
});

test('deletion marker rejects companion updates and profile recreation from stale owner', async () => {
  const owner = env.authenticatedContext('owner').firestore();
  const fields = defaultLocationFields();
  await setDoc(doc(owner, 'users/owner'), fields);
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'account_deletions/owner'), { blocked: true });
  });
  await assertFails(setDoc(doc(owner, 'users/owner'), fields, { merge: true }));
  await assertFails(getDoc(doc(owner, 'users/owner')));
  await env.withSecurityRulesDisabled(async context => {
    assert.deepEqual((await getDoc(doc(context.firestore(), 'users/owner'))).data(), fields);
  });
  // A different deleted account with no profile must not be able to recreate it.
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'account_deletions/deleted'), { blocked: true });
  });
  const deleted = env.authenticatedContext('deleted').firestore();
  await assertFails(setDoc(doc(deleted, 'users/deleted'), fields, { merge: true }));
});

test('legacy name-only writes retain stale companion: compatibility limitation is explicit', async () => {
  const db = env.authenticatedContext('owner').firestore();
  const profile = doc(db, 'users/owner');
  const fields = defaultLocationFields();
  await setDoc(profile, fields);
  await setDoc(profile, { defaultCity: 'Ankara' }, { merge: true });
  const saved = (await getDoc(profile)).data();
  assert.equal(saved.defaultCity, 'Ankara');
  assert.deepEqual(saved.defaultCityLocation, fields.defaultCityLocation);
});


const invalidDefaultLocations = [
  ['unsupported version', { version: 2 }],
  ['fractional ID', { id: 1.5 }],
  ['blank name', { name: '' }],
  ['conflicting name', { name: 'Ankara' }],
  ['out of range latitude', { latitude: 91 }],
  ['out of range longitude', { longitude: -181 }],
  ['non-finite latitude', { latitude: NaN }],
  ['infinite longitude', { longitude: Infinity }],
  ['string coordinate', { latitude: '48.8' }],
  ['invalid country code', { country_code: 42 }],
  ['invalid administrative area', { admin1: [] }],
  ['unknown field', { unexpected: true }]
];
for (const [label, patch] of invalidDefaultLocations) {
  test(`default location rejects ${label} on create and update without data loss`, async () => {
    const db = env.authenticatedContext('owner').firestore();
    const profile = doc(db, 'users/owner');
    const original = { displayName: 'Preserved', ...defaultLocationFields() };
    const bad = { ...defaultLocationFields(), defaultCityLocation: {
      ...defaultLocationFields().defaultCityLocation, ...patch
    }};
    await assertFails(setDoc(profile, bad));
    assert.equal((await getDoc(profile)).exists(), false);
    await setDoc(profile, original);
    await assertFails(setDoc(profile, bad, { merge: true }));
    assert.deepEqual((await getDoc(profile)).data(), original);
  });
}

test('default location rejects incomplete and non-map companions', async () => {
  const db = env.authenticatedContext('owner').firestore();
  const profile = doc(db, 'users/owner');
  for (const value of [null, 'Paris', [], { version: 1, id: 1, name: 'Paris' }]) {
    await assertFails(setDoc(profile, { defaultCity: 'Paris', defaultCityLocation: value }));
  }
  await assertFails(setDoc(profile, { defaultCityLocation: defaultLocationFields().defaultCityLocation }));
});

test('legacy malformed companion does not block unrelated profile edits and can be replaced or removed', async () => {
  const db = env.authenticatedContext('owner').firestore();
  const profile = doc(db, 'users/owner');
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'users/owner'), { defaultCity: 'Paris', defaultCityLocation: 'old' });
  });
  await assertSucceeds(setDoc(profile, { displayName: 'Updated' }, { merge: true }));
  await assertSucceeds(setDoc(profile, defaultLocationFields(), { merge: true }));
  assert.equal((await getDoc(profile)).data().displayName, 'Updated');
  await assertSucceeds(setDoc(profile, { defaultCityLocation: deleteField() }, { merge: true }));
  assert.equal('defaultCityLocation' in (await getDoc(profile)).data(), false);
  await assertSucceeds(deleteDoc(profile));
});

test('manual zero ID and optional null metadata remain valid', async () => {
  const db = env.authenticatedContext('owner').firestore();
  const fields = defaultLocationFields();
  fields.defaultCityLocation.id = 0;
  await assertSucceeds(setDoc(doc(db, 'users/owner'), fields));
  assert.deepEqual((await getDoc(doc(db, 'users/owner'))).data(), fields);
});


for (const operation of ['delete', 'create', 'recreate-after-cleanup']) {
  test(`deletion marker wins against an already-read city transaction: ${operation}`, { timeout: 20_000 }, async () => {
    const owner = env.authenticatedContext('owner').firestore();
    const city = doc(owner, 'users/owner/cities/1');
    const original = { id: 1, name: 'Paris', latitude: 48.8, longitude: 2.3, country_code: 'FR' };
    if (operation !== 'create') await setDoc(city, original);
    let announceRead;
    let releaseRead;
    const readReached = new Promise(resolve => { announceRead = resolve; });
    const release = new Promise(resolve => { releaseRead = resolve; });
    let staged = 0;
    // Attach rejection handling immediately; a denied transaction must not become an unhandled promise.
    const denied = assertFails(runTransaction(owner, async tx => {
      const snapshot = await tx.get(city);
      announceRead();
      await release;
      if (operation === 'delete') {
        assert.equal(snapshot.exists(), true);
        tx.delete(city);
      } else {
        tx.set(city, { ...original, name: 'Attempted stale write' });
      }
      staged++;
    }));
    let timer;
    try {
      await Promise.race([readReached, new Promise((_, reject) => {
        timer = setTimeout(() => reject(new Error('Transaction did not reach controlled read')), 10_000);
      })]);
      await env.withSecurityRulesDisabled(async context => {
        const admin = context.firestore();
        await setDoc(doc(admin, 'account_deletions/owner'), { blocked: true });
        if (operation === 'recreate-after-cleanup') await deleteDoc(doc(admin, 'users/owner/cities/1'));
      });
    } finally {
      clearTimeout(timer);
      releaseRead();
    }
    const error = await denied;
    assert.equal(error.code, 'permission-denied');
    assert.equal(staged, 1, 'The old authenticated context must have staged a write after its successful read');
    await env.withSecurityRulesDisabled(async context => {
      const snapshot = await getDoc(doc(context.firestore(), 'users/owner/cities/1'));
      if (operation === 'delete') assert.deepEqual(snapshot.data(), original);
      else assert.equal(snapshot.exists(), false, 'Stale client recreated data after deletion marker');
      assert.equal((await getDoc(doc(context.firestore(), 'account_deletions/owner'))).exists(), true);
    });
    await assertFails(getDoc(city));
  });
}


for (const field of ['isPremium', 'premium']) {
  test(`client cannot grant ${field} on create or update`, async () => {
    const db = env.authenticatedContext('owner').firestore();
    const profile = doc(db, 'users/owner');
    await assertFails(setDoc(profile, { [field]: true }));
    await assertSucceeds(setDoc(profile, { name: 'Free', [field]: false }));
    await assertFails(setDoc(profile, { [field]: true }, { merge: true }));
    assert.deepEqual((await getDoc(profile)).data(), { name: 'Free', [field]: false });
  });
  test(`server ${field} survives ordinary edits and cannot be changed or removed`, async () => {
    const db = env.authenticatedContext('owner').firestore();
    const profile = doc(db, 'users/owner');
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), 'users/owner'), { name: 'Paid', [field]: true });
    });
    await assertSucceeds(setDoc(profile, { name: 'Renamed' }, { merge: true }));
    await assertFails(setDoc(profile, { [field]: false }, { merge: true }));
    await assertFails(setDoc(profile, { [field]: deleteField() }, { merge: true }));
    assert.deepEqual((await getDoc(profile)).data(), { name: 'Renamed', [field]: true });
  });
}
test('client cannot add a conflicting alias beside a confirmed server mirror', async () => {
  const db = env.authenticatedContext('owner').firestore();
  for (const [field, other] of [['isPremium', 'premium'], ['premium', 'isPremium']]) {
    await env.withSecurityRulesDisabled(async context => {
      await setDoc(doc(context.firestore(), 'users/owner'), { [field]: true });
    });
    await assertFails(setDoc(doc(db, 'users/owner'), { [other]: false }, { merge: true }));
    await assertFails(setDoc(doc(db, 'users/owner'), { [other]: true }, { merge: true }));
  }
});
test('legacy free initialization and unrelated edits remain compatible', async () => {
  const db = env.authenticatedContext('owner').firestore();
  const profile = doc(db, 'users/owner');
  await assertSucceeds(setDoc(profile, { name: 'Free' }));
  await assertSucceeds(setDoc(profile, { premium: false }, { merge: true }));
  await assertSucceeds(setDoc(profile, { isPremium: false }, { merge: true }));
  await assertSucceeds(setDoc(profile, { name: 'Edited', bio: 'Preserved' }, { merge: true }));
  assert.deepEqual((await getDoc(profile)).data(), { name: 'Edited', bio: 'Preserved', premium: false, isPremium: false });
});
test('malformed premium types are rejected without blocking unrelated legacy edits', async () => {
  const db = env.authenticatedContext('owner').firestore();
  for (const field of ['isPremium', 'premium']) for (const value of [null, 'false', 0]) {
    await assertFails(setDoc(doc(db, 'users/owner'), { [field]: value }));
  }
  await env.withSecurityRulesDisabled(async context => {
    await setDoc(doc(context.firestore(), 'users/owner'), { premium: 'old-invalid' });
  });
  await assertSucceeds(setDoc(doc(db, 'users/owner'), { name: 'Edited' }, { merge: true }));
});
