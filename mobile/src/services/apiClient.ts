import axios from 'axios';
import {Platform} from 'react-native';

// Android emülatöründe localhost host makinasına ulaşamaz; 10.0.2.2 kullanılmalı
const API_BASE_URL = Platform.OS === 'android'
  ? 'http://10.0.2.2:8000/v1'
  : 'http://localhost:8000/v1';

let tokenRefreshHandler: null | (() => Promise<string | null>) = null;

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

export function setAccessToken(token: string | null): void {
  if (token) {
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete apiClient.defaults.headers.common.Authorization;
  }
}

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
    if (!newAccessToken) {
      return Promise.reject(error);
    }

    error.config.headers.Authorization = `Bearer ${newAccessToken}`;
    return apiClient.request(error.config);
  },
);
