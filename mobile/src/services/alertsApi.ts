import {apiClient} from './apiClient';

export async function getAlerts(params?: {severity?: string; location_id?: string}) {
  const response = await apiClient.get('/alerts', {params: {...params, grouped: true}});
  return response.data;
}

export async function getAlertDetail(alertId: string) {
  const response = await apiClient.get(`/alerts/${alertId}`);
  return response.data;
}
