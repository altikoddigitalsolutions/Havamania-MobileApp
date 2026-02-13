import {apiClient} from './apiClient';

export async function getProfile() {
  const response = await apiClient.get('/profile');
  return response.data;
}

export async function updateProfile(payload: Record<string, unknown>) {
  const response = await apiClient.patch('/profile', payload);
  return response.data;
}

export async function getLocations() {
  const response = await apiClient.get('/profile/locations');
  return response.data;
}

export async function createLocation(payload: Record<string, unknown>) {
  const response = await apiClient.post('/profile/locations', payload);
  return response.data;
}

export async function updateLocation(locationId: string, payload: Record<string, unknown>) {
  const response = await apiClient.patch(`/profile/locations/${locationId}`, payload);
  return response.data;
}

export async function deleteLocation(locationId: string) {
  const response = await apiClient.delete(`/profile/locations/${locationId}`);
  return response.data;
}

export async function getNotificationPreferences() {
  const response = await apiClient.get('/profile/notifications');
  return response.data;
}

export async function updateNotificationPreferences(payload: Record<string, unknown>) {
  const response = await apiClient.patch('/profile/notifications', payload);
  return response.data;
}
