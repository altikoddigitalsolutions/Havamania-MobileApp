import {apiClient} from './apiClient';

export type SubscriptionStatus = {
  plan_code: string;
  status: string;
  expires_at: string | null;
  is_premium_active: boolean;
};

export async function getSubscriptionStatus(): Promise<SubscriptionStatus> {
  const response = await apiClient.get<SubscriptionStatus>('/subscription/status');
  return response.data;
}

export async function validateReceipt(payload: {
  store: 'ios' | 'android';
  receipt_data: string;
  plan_code: string;
}): Promise<SubscriptionStatus> {
  const response = await apiClient.post<SubscriptionStatus>('/subscription/validate-receipt', payload);
  return response.data;
}
