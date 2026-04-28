import axios from 'axios';
import {Platform} from 'react-native';

/**
 * ÖNEMLİ: Fiziksel cihaz (Tablet) bağlantısı için:
 * 1. Bilgisayarınızın IPv4 adresini terminalden 'ipconfig' ile bulun.
 * 2. Aşağıdaki IP adresini kendi adresinizle değiştirin.
 * 3. Tablet ve Bilgisayarın AYNI WI-FI ağına bağlı olduğundan emin olun.
 */
const COMPUTER_IP = '192.168.1.50'; // <--- BURAYI KENDİ IP ADRESİNLE DEĞİŞTİR

export const BASE_URL = Platform.OS === 'android'
  ? `http://${COMPUTER_IP}:8000`
  : 'http://localhost:8000';

const API_BASE_URL = `${BASE_URL}/v1`;

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

// Geri kalan interceptor kodları...
export function setAccessToken(token: string | null): void {
  if (token) {
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete apiClient.defaults.headers.common.Authorization;
  }
}

let tokenRefreshHandler: null | (() => Promise<string | null>) = null;
export function setTokenRefreshHandler(handler: () => Promise<string | null>): void {
  tokenRefreshHandler = handler;
}

apiClient.interceptors.response.use(
  response => response,
  async error => {
    if (!error?.response || error.response.status !== 401 || error.config?._retry || !tokenRefreshHandler) {
      return Promise.reject(error);
    }
    error.config._retry = true;
    const newAccessToken = await tokenRefreshHandler();
    if (!newAccessToken) return Promise.reject(error);
    error.config.headers.Authorization = `Bearer ${newAccessToken}`;
    return apiClient.request(error.config);
  },
);
