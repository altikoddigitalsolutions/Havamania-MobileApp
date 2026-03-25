/**
 * Keychain tabanlı tercihler depolaması.
 * AsyncStorage gerekmeden, zaten kurulu olan react-native-keychain kullanır.
 */
import Keychain from 'react-native-keychain';

const SERVICE_KEY = 'havamania-prefs';
const USERNAME = 'prefs';

interface StoredPreferences {
  language?: string;
  avatarEmoji?: string;
  tempUnit?: string;
}

async function readPrefs(): Promise<StoredPreferences> {
  try {
    const creds = await Keychain.getGenericPassword({service: SERVICE_KEY});
    if (!creds) return {};
    return JSON.parse(creds.password) as StoredPreferences;
  } catch {
    return {};
  }
}

async function writePrefs(prefs: StoredPreferences): Promise<void> {
  try {
    await Keychain.setGenericPassword(USERNAME, JSON.stringify(prefs), {
      service: SERVICE_KEY,
    });
  } catch {
    // ignore — preferences are non-critical
  }
}

export async function getStoredLanguage(): Promise<string | null> {
  const prefs = await readPrefs();
  return prefs.language ?? null;
}

export async function saveLanguage(lang: string): Promise<void> {
  const prefs = await readPrefs();
  await writePrefs({...prefs, language: lang});
}

export async function getStoredAvatarEmoji(): Promise<string | null> {
  const prefs = await readPrefs();
  return prefs.avatarEmoji ?? null;
}

export async function saveAvatarEmoji(emoji: string): Promise<void> {
  const prefs = await readPrefs();
  await writePrefs({...prefs, avatarEmoji: emoji});
}
