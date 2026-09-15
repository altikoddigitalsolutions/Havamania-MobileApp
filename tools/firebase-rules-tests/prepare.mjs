import { copyFile, mkdir } from 'node:fs/promises';

// Firebase CLI requires rule paths inside its project directory.
// Copy from the actual application rules before every run; never maintain a second version.
await mkdir(new URL('.generated/', import.meta.url), {recursive: true});
for (const file of ['firestore.rules', 'storage.rules']) {
  await copyFile(new URL(`../../mobile/firebase-rules/${file}`, import.meta.url),
    new URL(`.generated/${file}`, import.meta.url));
}
