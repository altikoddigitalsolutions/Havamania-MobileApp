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
    // Kullanıcı dostu hata mesajları ekle
    if (!error.response) {
      if (error.code === 'ECONNABORTED') {
        error.friendlyMessage = 'İstek zaman aşımına uğradı. Lütfen tekrar dene.';
      } else {
        error.friendlyMessage = 'İnternet bağlantısı sorunu yaşıyoruz. Lütfen bağlantını kontrol et.';
      }
    } else {
      const status = error.response.status;
      if (status === 401) {
        // Refresh token mantığı burada (zaten var)
        if (error.config?._retry || !tokenRefreshHandler) {
          return Promise.reject(error);
        }
        error.config._retry = true;
        const newAccessToken = await tokenRefreshHandler();
        if (!newAccessToken) return Promise.reject(error);
        error.config.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient.request(error.config);
      } else if (status === 404) {
        error.friendlyMessage = 'İstediğin bilgiye şu an ulaşılamıyor.';
      } else if (status >= 500) {
        error.friendlyMessage = 'Sunucularımızda kısa süreli bir sorun var. Birazdan tekrar dene.';
      }
    }

    return Promise.reject(error);
  },
);
