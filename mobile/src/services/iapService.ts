export type PurchaseResult = {
  receiptData: string;
  store: 'ios' | 'android';
};

export async function initializeIAP(): Promise<void> {
  // TODO: integrate react-native-iap initConnection and product fetch.
}

export async function purchasePlan(planCode: string): Promise<PurchaseResult> {
  // TODO: replace with real IAP purchase flow.
  return {
    receiptData: `demo-receipt-${planCode}-${Date.now()}`,
    store: 'ios',
  };
}

export async function restorePurchases(): Promise<PurchaseResult | null> {
  // TODO: replace with real restore purchases flow.
  return {
    receiptData: `demo-restore-${Date.now()}`,
    store: 'ios',
  };
}
