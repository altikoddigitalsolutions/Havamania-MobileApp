import {apiClient, setAccessToken, setTokenRefreshHandler} from './apiClient';
import {clearTokens, readTokens, saveTokens} from './tokenStorage';

export type AuthTokens = {
  access_token: string;
  refresh_token: string;
  token_type: string;
};

export async function signup(email: string, password: string, fullName?: string): Promise<AuthTokens> {
  const response = await apiClient.post<AuthTokens>('/auth/signup', {
    email,
    password,
    full_name: fullName,
  });
  return response.data;
}

export async function login(email: string, password: string): Promise<AuthTokens> {
  const response = await apiClient.post<AuthTokens>('/auth/login', {email, password});
  return response.data;
}

export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', {refresh_token: refreshToken});
}

export async function refresh(refreshToken: string): Promise<AuthTokens> {
  const response = await apiClient.post<AuthTokens>('/auth/refresh', {refresh_token: refreshToken});
  return response.data;
}

export async function persistSession(tokens: AuthTokens): Promise<void> {
  await saveTokens(tokens.access_token, tokens.refresh_token);
  setAccessToken(tokens.access_token);
}

export async function clearSession(): Promise<void> {
  await clearTokens();
  setAccessToken(null);
}

export async function bootstrapSession(): Promise<{accessToken: string; refreshToken: string} | null> {
  console.log('[DEBUG] bootstrapSession: reading tokens...');
  const stored = await readTokens();
  console.log('[DEBUG] bootstrapSession: stored tokens:', stored ? 'EXISTS' : 'NONE');
  if (!stored) {
    return null;
  }

  setAccessToken(stored.accessToken);
  return stored;
}

export function registerRefreshInterceptor(
  onTokensUpdated: (accessToken: string, refreshToken: string) => Promise<void>,
  onSessionExpired: () => Promise<void>,
): void {
  setTokenRefreshHandler(async () => {
    const tokens = await readTokens();
    if (!tokens?.refreshToken) {
      await onSessionExpired();
      return null;
    }

    try {
      const refreshed = await refresh(tokens.refreshToken);
      await onTokensUpdated(refreshed.access_token, refreshed.refresh_token);
      return refreshed.access_token;
    } catch {
      await onSessionExpired();
      return null;
    }
  });
}
