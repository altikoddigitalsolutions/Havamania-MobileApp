import * as Keychain from 'react-native-keychain';

const SERVICE = 'havamania-auth';

export async function saveTokens(accessToken: string, refreshToken: string): Promise<void> {
  const value = JSON.stringify({accessToken, refreshToken});
  await Keychain.setGenericPassword('token', value, {service: SERVICE});
}

export async function readTokens(): Promise<{accessToken: string; refreshToken: string} | null> {
  const credentials = await Keychain.getGenericPassword({service: SERVICE});
  if (!credentials) {
    return null;
  }

  try {
    return JSON.parse(credentials.password) as {accessToken: string; refreshToken: string};
  } catch {
    return null;
  }
}

export async function clearTokens(): Promise<void> {
  await Keychain.resetGenericPassword({service: SERVICE});
}
